const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8084";

export async function apiRequest(path, token, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  });

  if (response.status === 401) {
    sessionStorage.removeItem("stayfolio.auth");
    window.dispatchEvent(new Event("stayfolio:expired"));
    throw new Error("Your session expired. Please sign in again.");
  }

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed with HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function hotelId(hotel) {
  return hotel.hotel_id || hotel.hotelId;
}

export { API_BASE_URL };
