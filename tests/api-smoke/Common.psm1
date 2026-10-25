$script:Gateway = 'http://localhost:8084'

if ([string]::IsNullOrWhiteSpace($env:API_ACCESS_TOKEN)) {
	throw 'Set API_ACCESS_TOKEN to a valid JWT before running API smoke tests.'
}

function Invoke-TestRequest {
	param(
		[Parameter(Mandatory)]
		[string]$Method,
		[Parameter(Mandatory)]
		[string]$Path,
		[object]$Body
	)

	$request = @{
		Uri = "$script:Gateway$Path"
		Method = $Method
		Headers = @{ Authorization = "Bearer $env:API_ACCESS_TOKEN" }
		SkipHttpErrorCheck = $true
		ErrorAction = 'Stop'
	}
	if ($PSBoundParameters.ContainsKey('Body')) {
		$request.ContentType = 'application/json'
		$request.Body = ConvertTo-Json -InputObject $Body -Depth 8 -Compress
	}

	$response = Invoke-WebRequest @request
	$content = $response.Content
	$parsedBody = $content
	if ($content -and $response.Headers['Content-Type'] -match 'application/json') {
		$parsedBody = ConvertFrom-Json -InputObject $content
	}

	return [pscustomobject]@{
		StatusCode = [int]$response.StatusCode
		Body = $parsedBody
		Content = $content
	}
}

function Assert-TestStatus {
	param(
		[Parameter(Mandatory)]
		[psobject]$Response,
		[Parameter(Mandatory)]
		[int]$Expected,
		[Parameter(Mandatory)]
		[string]$Operation
	)

	if ($Response.StatusCode -ne $Expected) {
		throw "$Operation expected HTTP $Expected but received $($Response.StatusCode): $($Response.Content)"
	}
	Write-Output "[PASS] $Operation -> HTTP $Expected"
}

function Assert-TestCondition {
	param(
		[Parameter(Mandatory)]
		[bool]$Condition,
		[Parameter(Mandatory)]
		[string]$Message
	)

	if (-not $Condition) {
		throw $Message
	}
	Write-Output "[PASS] $Message"
}

Export-ModuleMember -Function Invoke-TestRequest, Assert-TestStatus, Assert-TestCondition
