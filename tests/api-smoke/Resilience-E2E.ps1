$ErrorActionPreference = 'Stop'
$gateway = 'http://localhost:8084'
if ([string]::IsNullOrWhiteSpace($env:API_ACCESS_TOKEN)) {
	throw 'Set API_ACCESS_TOKEN to a valid JWT before running resilience tests.'
}
$suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
$rateLimitedUserId = "USR-RL-$suffix"
$failureUserId = "USR-CB-$suffix"
$failureRatingId = "REV-CB-$suffix"
$recoveryUserId = "USR-HALF-$suffix"
$recoveryHotelId = "HTL-HALF-$suffix"
$recoveryRatingId = "REV-HALF-$suffix"
$rateLimitedUserCreated = $false
$failureUserCreated = $false
$failureRatingCreated = $false
$recoveryUserCreated = $false
$recoveryHotelCreated = $false
$recoveryRatingCreated = $false
$client = [System.Net.Http.HttpClient]::new()
$client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
	'Bearer', $env:API_ACCESS_TOKEN)

function Invoke-JsonApi {
	param(
		[Parameter(Mandatory)]
		[string]$Method,
		[Parameter(Mandatory)]
		[string]$Path,
		[object]$Body
	)

	$httpMethod = [System.Net.Http.HttpMethod]::new($Method)
	$request = [System.Net.Http.HttpRequestMessage]::new($httpMethod, "$gateway$Path")
	if ($PSBoundParameters.ContainsKey('Body')) {
		$json = ConvertTo-Json -InputObject $Body -Depth 8 -Compress
		$request.Content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, 'application/json')
	}
	$response = $client.SendAsync($request).GetAwaiter().GetResult()
	$content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
	return [pscustomobject]@{
		StatusCode = [int]$response.StatusCode
		Content = $content
		Headers = $response.Headers
	}
}

try {
	$createUser = Invoke-JsonApi -Method POST -Path '/adduser' -Body @{
		userid = $rateLimitedUserId
		name = 'Rate limiter smoke test'
		email = "$suffix@example.test"
	}
	if ($createUser.StatusCode -ne 200) {
		throw "Rate limiter test user creation failed with HTTP $($createUser.StatusCode): $($createUser.Content)"
	}
	$rateLimitedUserCreated = $true
	Write-Output '[PASS] Created disposable rate-limit test user'

	$tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]]::new()
	for ($i = 0; $i -lt 160; $i++) {
		$tasks.Add($client.GetAsync("$gateway/users/$rateLimitedUserId"))
	}
	[System.Threading.Tasks.Task]::WhenAll([System.Threading.Tasks.Task[]]$tasks.ToArray()).GetAwaiter().GetResult() | Out-Null
	$responses = @($tasks | ForEach-Object { $_.Result })
	$statuses = $responses | ForEach-Object { [int]$_.StatusCode }
	$successCount = @($statuses | Where-Object { $_ -eq 200 }).Count
	$limitedCount = @($statuses | Where-Object { $_ -eq 429 }).Count
	$otherStatuses = @($statuses | Where-Object { $_ -notin @(200, 429) })
	if ($limitedCount -lt 1 -or $otherStatuses.Count -gt 0) {
		throw "Expected only HTTP 200 and at least one HTTP 429 during a 160-request burst; received: $($statuses -join ', ')"
	}
	$retryAfter = @($responses | Where-Object { [int]$_.StatusCode -eq 429 } | Select-Object -First 1)[0].Headers.RetryAfter
	if ($null -eq $retryAfter -or $retryAfter.Delta.TotalSeconds -lt 1) {
		throw 'HTTP 429 response did not include the configured Retry-After header'
	}
	Write-Output "[PASS] Rate limiter burst: $successCount HTTP 200, $limitedCount HTTP 429, Retry-After $($retryAfter.Delta.TotalSeconds)s"

	$failureUser = Invoke-JsonApi -Method POST -Path '/adduser' -Body @{
		userid = $failureUserId
		name = 'Circuit breaker smoke test'
		email = "cb-$suffix@example.test"
	}
	if ($failureUser.StatusCode -ne 200) {
		throw "Circuit breaker test user creation failed with HTTP $($failureUser.StatusCode): $($failureUser.Content)"
	}
	$failureUserCreated = $true

	$failureRating = Invoke-JsonApi -Method POST -Path '/ratings' -Body @{
		ratingId = $failureRatingId
		hotelId = "MISSING-$suffix"
		userid = $failureUserId
		rating = 3
		feedback = 'Temporary missing-dependency test record'
	}
	if ($failureRating.StatusCode -ne 201) {
		throw "Circuit breaker test rating creation failed with HTTP $($failureRating.StatusCode): $($failureRating.Content)"
	}
	$failureRatingCreated = $true

	Start-Sleep -Seconds 1
	for ($i = 1; $i -le 5; $i++) {
		$dependencyFailure = Invoke-JsonApi -Method GET -Path "/users/$failureUserId"
		if ($dependencyFailure.StatusCode -ne 503) {
			throw "Expected HTTP 503 for downstream failure $i, received HTTP $($dependencyFailure.StatusCode): $($dependencyFailure.Content)"
		}
	}
	$breakerState = (Invoke-RestMethod -Uri 'http://localhost:8081/actuator/circuitbreakers' -Headers @{
		Authorization = "Bearer $env:API_ACCESS_TOKEN"
	}).circuitBreakers.hotelService.state
	if ($breakerState -ne 'OPEN') {
		throw "Expected User hotelService circuit breaker to open after 5 failed calls; state is $breakerState"
	}
	$openCircuitResponse = Invoke-JsonApi -Method GET -Path "/users/$failureUserId"
	if ($openCircuitResponse.StatusCode -ne 503) {
		throw "Expected HTTP 503 when circuit is open, received HTTP $($openCircuitResponse.StatusCode)"
	}
	Write-Output '[PASS] Five downstream failures open the circuit; calls rejected by the open circuit return HTTP 503'

	$recoveryHotel = Invoke-JsonApi -Method POST -Path '/hotels' -Body @{
		hotel_id = $recoveryHotelId
		name = 'Circuit recovery test'
		location = 'Test City'
		about = 'Temporary circuit recovery record'
	}
	if ($recoveryHotel.StatusCode -ne 201) {
		throw "Recovery hotel creation failed with HTTP $($recoveryHotel.StatusCode)"
	}
	$recoveryHotelCreated = $true
	$recoveryRating = Invoke-JsonApi -Method POST -Path '/ratings' -Body @{
		ratingId = $recoveryRatingId
		hotelId = $recoveryHotelId
		userid = $recoveryUserId
		rating = 5
		feedback = 'Temporary circuit recovery record'
	}
	if ($recoveryRating.StatusCode -ne 201) {
		throw "Recovery rating creation failed with HTTP $($recoveryRating.StatusCode)"
	}
	$recoveryRatingCreated = $true
	$recoveryUser = Invoke-JsonApi -Method POST -Path '/adduser' -Body @{
		userid = $recoveryUserId
		name = 'Circuit recovery test'
		email = "recovery-$suffix@example.test"
	}
	if ($recoveryUser.StatusCode -ne 200) {
		throw "Recovery user creation failed with HTTP $($recoveryUser.StatusCode)"
	}
	$recoveryUserCreated = $true

	Start-Sleep -Seconds 11
	for ($i = 1; $i -le 3; $i++) {
		$recovery = Invoke-JsonApi -Method GET -Path "/users/$recoveryUserId"
		if ($recovery.StatusCode -ne 200) {
			throw "Expected successful half-open recovery probe $i, received HTTP $($recovery.StatusCode): $($recovery.Content)"
		}
	}
	$breakerState = (Invoke-RestMethod -Uri 'http://localhost:8081/actuator/circuitbreakers' -Headers @{
		Authorization = "Bearer $env:API_ACCESS_TOKEN"
	}).circuitBreakers.hotelService.state
	if ($breakerState -ne 'CLOSED') {
		throw "Expected circuit breaker to close after three successful half-open probes; state is $breakerState"
	}
	Write-Output '[PASS] Three successful half-open probes close the circuit'
}
finally {
	if ($failureUserCreated) {
		Invoke-JsonApi -Method DELETE -Path "/users/$failureUserId" | Out-Null
	}
	if ($failureRatingCreated) {
		Invoke-JsonApi -Method DELETE -Path "/ratings/$failureRatingId" | Out-Null
	}
	if ($rateLimitedUserCreated) {
		Invoke-JsonApi -Method DELETE -Path "/users/$rateLimitedUserId" | Out-Null
	}
	if ($recoveryUserCreated) {
		Invoke-JsonApi -Method DELETE -Path "/users/$recoveryUserId" | Out-Null
	}
	if ($recoveryRatingCreated) {
		Invoke-JsonApi -Method DELETE -Path "/ratings/$recoveryRatingId" | Out-Null
	}
	if ($recoveryHotelCreated) {
		Invoke-JsonApi -Method DELETE -Path "/hotels/$recoveryHotelId" | Out-Null
	}
	$client.Dispose()
}
