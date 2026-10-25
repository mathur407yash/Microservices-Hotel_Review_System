$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot\Common.psm1" -Force

$suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
$hotelId = "HTL-$suffix"
$ratingId = "REV-$suffix"
$ratingCreated = $false
$hotelCreated = $false

try {
	$hotel = Invoke-TestRequest -Method POST -Path '/hotels' -Body @{
		hotel_id = $hotelId
		name = 'API smoke hotel'
		location = 'Test City'
		about = 'Temporary API smoke test record'
	}
	Assert-TestStatus $hotel 201 'Create hotel'
	$hotelCreated = $true

	$rating = Invoke-TestRequest -Method POST -Path '/ratings' -Body @{
		ratingId = $ratingId
		hotelId = $hotelId
		userid = "USR-$suffix"
		rating = 4
		feedback = 'Temporary API smoke test rating'
	}
	Assert-TestStatus $rating 201 'Create hotel rating'
	$ratingCreated = $true

	$hotels = Invoke-TestRequest -Method GET -Path '/hotels'
	Assert-TestStatus $hotels 200 'List hotels'
	Assert-TestCondition (@($hotels.Body | Where-Object hotel_id -eq $hotelId).Count -eq 1) 'Created hotel is persisted in hotel list'

	$hotelWithRatings = Invoke-TestRequest -Method GET -Path "/hotels/$hotelId"
	Assert-TestStatus $hotelWithRatings 200 'Get hotel with ratings'
	Assert-TestCondition ($hotelWithRatings.Body.hotel.hotel_id -eq $hotelId) 'Hotel detail identifies requested hotel'
	Assert-TestCondition (@($hotelWithRatings.Body.ratings | Where-Object ratingId -eq $ratingId).Count -eq 1) 'Hotel detail includes matching rating'

	$details = Invoke-TestRequest -Method GET -Path "/hotels/$hotelId/details"
	Assert-TestStatus $details 200 'Get hotel details'
	Assert-TestCondition ($details.Body.hotel_id -eq $hotelId) 'Hotel details response identifies requested hotel'

	$ratings = Invoke-TestRequest -Method GET -Path "/hotels/$hotelId/ratings"
	Assert-TestStatus $ratings 200 'Get hotel ratings'
	Assert-TestCondition (@($ratings.Body.ratings | Where-Object ratingId -eq $ratingId).Count -eq 1) 'Hotel ratings endpoint includes matching rating'

	$deleteHotel = Invoke-TestRequest -Method DELETE -Path "/hotels/$hotelId"
	Assert-TestStatus $deleteHotel 204 'Delete hotel'
	$hotelCreated = $false
	$hotelsAfterDelete = Invoke-TestRequest -Method GET -Path '/hotels'
	Assert-TestStatus $hotelsAfterDelete 200 'List hotels after delete'
	Assert-TestCondition (@($hotelsAfterDelete.Body | Where-Object hotel_id -eq $hotelId).Count -eq 0) 'Deleted hotel is absent from hotel list'
}
finally {
	if ($ratingCreated) {
		Invoke-TestRequest -Method DELETE -Path "/ratings/$ratingId" | Out-Null
	}
	if ($hotelCreated) {
		Invoke-TestRequest -Method DELETE -Path "/hotels/$hotelId" | Out-Null
	}
}
