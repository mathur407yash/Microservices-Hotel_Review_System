$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot\Common.psm1" -Force

$suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
$ratingId = "REV-$suffix"
$userId = "USR-$suffix"
$hotelId = "HTL-$suffix"
$ratingCreated = $false

try {
	$rating = Invoke-TestRequest -Method POST -Path '/ratings' -Body @{
		ratingId = $ratingId
		hotelId = $hotelId
		userid = $userId
		rating = 5
		feedback = 'Temporary API smoke test rating'
	}
	Assert-TestStatus $rating 201 'Create rating'
	$ratingCreated = $true

	$ratings = Invoke-TestRequest -Method GET -Path '/ratings'
	Assert-TestStatus $ratings 200 'List ratings'
	Assert-TestCondition (@($ratings.Body | Where-Object ratingId -eq $ratingId).Count -eq 1) 'Created rating is persisted in rating list'

	$byId = Invoke-TestRequest -Method GET -Path "/ratings/$ratingId"
	Assert-TestStatus $byId 200 'Get rating by id'
	Assert-TestCondition ($byId.Body.ratingId -eq $ratingId -and $byId.Body.hotelId -eq $hotelId) 'Rating detail matches submitted data'

	$byUser = Invoke-TestRequest -Method GET -Path "/ratings/user/$userId"
	Assert-TestStatus $byUser 200 'Get ratings by user'
	Assert-TestCondition (@($byUser.Body | Where-Object ratingId -eq $ratingId).Count -eq 1) 'User rating filter returns created rating'

	$byHotel = Invoke-TestRequest -Method GET -Path "/ratings/hotel/$hotelId"
	Assert-TestStatus $byHotel 200 'Get ratings by hotel'
	Assert-TestCondition (@($byHotel.Body | Where-Object ratingId -eq $ratingId).Count -eq 1) 'Hotel rating filter returns created rating'

	$deleteRating = Invoke-TestRequest -Method DELETE -Path "/ratings/$ratingId"
	Assert-TestStatus $deleteRating 204 'Delete rating'
	$ratingCreated = $false
	$ratingsAfterDelete = Invoke-TestRequest -Method GET -Path '/ratings'
	Assert-TestStatus $ratingsAfterDelete 200 'List ratings after delete'
	Assert-TestCondition (@($ratingsAfterDelete.Body | Where-Object ratingId -eq $ratingId).Count -eq 0) 'Deleted rating is absent from rating list'
}
finally {
	if ($ratingCreated) {
		Invoke-TestRequest -Method DELETE -Path "/ratings/$ratingId" | Out-Null
	}
}
