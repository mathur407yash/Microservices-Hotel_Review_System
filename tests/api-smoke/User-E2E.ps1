$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot\Common.psm1" -Force

$suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
$userId = "USR-$suffix"
$hotelId = "HTL-$suffix"
$ratingId = "REV-$suffix"
$userCreated = $false
$hotelCreated = $false
$ratingCreated = $false

try {
	$hotel = Invoke-TestRequest -Method POST -Path '/hotels' -Body @{
		hotel_id = $hotelId
		name = 'API smoke hotel'
		location = 'Test City'
		about = 'Temporary API smoke test record'
	}
	Assert-TestStatus $hotel 201 'Create backing hotel'
	$hotelCreated = $true

	$rating = Invoke-TestRequest -Method POST -Path '/ratings' -Body @{
		ratingId = $ratingId
		hotelId = $hotelId
		userid = $userId
		rating = 5
		feedback = 'Temporary API smoke test rating'
	}
	Assert-TestStatus $rating 201 'Create backing rating'
	$ratingCreated = $true

	$createUser = Invoke-TestRequest -Method POST -Path '/adduser' -Body @{
		userid = $userId
		name = 'API Smoke User'
		email = "$suffix@example.test"
	}
	Assert-TestStatus $createUser 200 'Create user'
	Assert-TestCondition ($createUser.Body -eq 'User added successfully') 'User create response body'
	$userCreated = $true

	$users = Invoke-TestRequest -Method GET -Path '/users'
	Assert-TestStatus $users 200 'List users'
	Assert-TestCondition (@($users.Body | Where-Object userid -eq $userId).Count -eq 1) 'Created user is persisted in user list'

	$user = Invoke-TestRequest -Method GET -Path "/users/$userId"
	Assert-TestStatus $user 200 'Get user with ratings'
	Assert-TestCondition ($user.Body.user.userid -eq $userId) 'User detail identifies requested user'
	Assert-TestCondition (@($user.Body.ratings | Where-Object { $_.rating.ratingId -eq $ratingId -and $_.hotel.hotel_id -eq $hotelId }).Count -eq 1) 'User aggregation includes rating and hotel details'

	$deleteUser = Invoke-TestRequest -Method DELETE -Path "/users/$userId"
	Assert-TestStatus $deleteUser 204 'Delete user'
	$userCreated = $false
	$usersAfterDelete = Invoke-TestRequest -Method GET -Path '/users'
	Assert-TestStatus $usersAfterDelete 200 'List users after delete'
	Assert-TestCondition (@($usersAfterDelete.Body | Where-Object userid -eq $userId).Count -eq 0) 'Deleted user is absent from user list'
}
finally {
	if ($userCreated) {
		Invoke-TestRequest -Method DELETE -Path "/users/$userId" | Out-Null
	}
	if ($ratingCreated) {
		Invoke-TestRequest -Method DELETE -Path "/ratings/$ratingId" | Out-Null
	}
	if ($hotelCreated) {
		Invoke-TestRequest -Method DELETE -Path "/hotels/$hotelId" | Out-Null
	}
}
