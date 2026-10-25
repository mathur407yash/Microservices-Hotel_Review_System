$ErrorActionPreference = 'Stop'

$protectedEndpoints = @(
	@{ Name = 'Gateway User API'; Uri = 'http://localhost:8084/users' }
	@{ Name = 'Gateway Hotel API'; Uri = 'http://localhost:8084/hotels' }
	@{ Name = 'Gateway Rating API'; Uri = 'http://localhost:8084/ratings' }
	@{ Name = 'Direct User Service'; Uri = 'http://localhost:8081/users' }
	@{ Name = 'Direct Hotel Service'; Uri = 'http://localhost:8082/hotels' }
	@{ Name = 'Direct Rating Service'; Uri = 'http://localhost:8083/ratings' }
)

foreach ($endpoint in $protectedEndpoints) {
	$response = Invoke-WebRequest -Uri $endpoint.Uri -SkipHttpErrorCheck -TimeoutSec 10
	if ([int]$response.StatusCode -ne 401) {
		throw "$($endpoint.Name) expected HTTP 401 without a bearer token but received HTTP $($response.StatusCode)."
	}
	Write-Output "[PASS] $($endpoint.Name) rejects anonymous access"
}

$publicHealth = Invoke-WebRequest -Uri 'http://localhost:8081/actuator/health' -SkipHttpErrorCheck -TimeoutSec 10
if ([int]$publicHealth.StatusCode -eq 401) {
	throw 'User Service health check unexpectedly requires a bearer token.'
}
Write-Output "[PASS] User Service health check is accessible without a bearer token (HTTP $([int]$publicHealth.StatusCode))"
