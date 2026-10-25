import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, API_BASE_URL, hotelId } from "./api.js";

const AUTH_STORAGE_KEY = "stayfolio.auth";

function readStoredAuth() {
  try {
    const stored = sessionStorage.getItem(AUTH_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

function App() {
  const [auth, setAuth] = useState(readStoredAuth);
  const [selectedRole, setSelectedRole] = useState("USER");
  const [loginError, setLoginError] = useState("");
  const popupRef = useRef(null);

  const signOut = useCallback(() => {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    setAuth(null);
    setLoginError("");
  }, []);

  useEffect(() => {
    const onAuthMessage = (event) => {
      if (
        event.origin !== new URL(API_BASE_URL).origin ||
        event.source !== popupRef.current ||
        event.data?.type !== "hotel-review-auth" ||
        typeof event.data.accessToken !== "string" ||
        !["USER", "HOTEL_OWNER"].includes(event.data.role)
      ) {
        return;
      }

      const nextAuth = {
        accessToken: event.data.accessToken,
        role: event.data.role,
        name: event.data.name || "",
        email: event.data.email || "",
      };
      sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuth));
      setAuth(nextAuth);
      setLoginError("");
      popupRef.current = null;
    };

    const onSessionExpired = () => signOut();
    window.addEventListener("message", onAuthMessage);
    window.addEventListener("stayfolio:expired", onSessionExpired);
    return () => {
      window.removeEventListener("message", onAuthMessage);
      window.removeEventListener("stayfolio:expired", onSessionExpired);
    };
  }, [signOut]);

  function startGoogleSignIn(role) {
    setSelectedRole(role);
    setLoginError("");
    const loginUrl = new URL("/oauth2/authorization/google", API_BASE_URL);
    loginUrl.searchParams.set("role", role);
    const popup = window.open(
      loginUrl,
      "stayfolio-google-login",
      "popup,width=520,height=680",
    );
    if (!popup) {
      setLoginError("Allow pop-ups for this site, then try Google sign-in again.");
      return;
    }
    popupRef.current = popup;
  }

  if (!auth) {
    return (
      <LoginScreen
        selectedRole={selectedRole}
        onSelectRole={setSelectedRole}
        onSignIn={startGoogleSignIn}
        error={loginError}
      />
    );
  }

  return (
    <Dashboard auth={auth} onSignOut={signOut} />
  );
}

function Brand({ compact = false }) {
  return (
    <a className={`brand${compact ? " brand-compact" : ""}`} href="/" aria-label="Stayfolio home">
      <span className="brand-mark" aria-hidden="true">
        <span />
        <span />
        <span />
      </span>
      <span>stayfolio</span>
    </a>
  );
}

function LoginScreen({ selectedRole, onSelectRole, onSignIn, error }) {
  return (
    <main className="login-page">
      <header className="topbar">
        <Brand />
        <span className="topbar-note">A better kind of stay</span>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow"><span className="eyebrow-dot" /> THE WORLD, WELL STAYED</span>
          <h1>Good places.<br /><em>Good people.</em><br />Better stories.</h1>
          <p className="hero-description">
            Find the stay that feels right, or welcome someone into a place of your own.
          </p>
          <div className="hero-meta">
            <span className="meta-icon" aria-hidden="true">✳</span>
            <span>Thoughtful stays, honest reviews</span>
            <span className="meta-divider" />
            <span>Made for hosts and travellers</span>
          </div>
        </div>

        <div className="login-panel">
          <div className="panel-heading">
            <span className="step-label">GET STARTED · 01</span>
            <h2>How will you stay?</h2>
            <p>Choose your space. You can always start fresh later.</p>
          </div>

          <div className="role-options" role="radiogroup" aria-label="Choose your account type">
            <button
              className={`role-card${selectedRole === "USER" ? " selected" : ""}`}
              type="button"
              role="radio"
              aria-checked={selectedRole === "USER"}
              onClick={() => onSelectRole("USER")}
            >
              <span className="role-icon traveller-icon" aria-hidden="true">↗</span>
              <span className="role-text">
                <strong>I’m here to explore</strong>
                <small>Find hotels, read reviews, share your stay.</small>
              </span>
              <span className="radio-indicator" aria-hidden="true" />
            </button>

            <button
              className={`role-card${selectedRole === "HOTEL_OWNER" ? " selected" : ""}`}
              type="button"
              role="radio"
              aria-checked={selectedRole === "HOTEL_OWNER"}
              onClick={() => onSelectRole("HOTEL_OWNER")}
            >
              <span className="role-icon host-icon" aria-hidden="true">⌂</span>
              <span className="role-text">
                <strong>I have a place to share</strong>
                <small>List your hotel and see what guests are saying.</small>
              </span>
              <span className="radio-indicator" aria-hidden="true" />
            </button>
          </div>

          <button className="google-button" type="button" onClick={() => onSignIn(selectedRole)}>
            <GoogleMark />
            <span>Continue with Google</span>
            <span className="button-arrow" aria-hidden="true">↗</span>
          </button>
          {error && <p className="error-message" role="alert">{error}</p>}
          <p className="fine-print">
            By continuing, you agree to our <a href="#terms">Terms</a> and <a href="#privacy">Privacy Policy</a>.
          </p>
          <div className="secure-note"><span aria-hidden="true">◈</span> Your sign-in is secured by Google</div>
        </div>
      </section>

      <footer className="login-footer">
        <span>© 2026 Stayfolio</span>
        <span>Make room for a good story.</span>
        <span>Designed for the way we travel <span aria-hidden="true">✳</span></span>
      </footer>
    </main>
  );
}

function GoogleMark() {
  return (
    <svg className="google-mark" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#FFC107" d="M43.6 24.5c0-1.4-.1-2.8-.4-4.1H24v7.8h11a9.4 9.4 0 0 1-4.1 6.2v5.1h6.7c3.9-3.6 6-8.9 6-15Z" />
      <path fill="#FF3D00" d="M24 44c5.5 0 10.1-1.8 13.5-4.8l-6.7-5.1c-1.8 1.2-4.1 1.9-6.8 1.9-5.2 0-9.6-3.5-11.2-8.1H5.9v5.3A20 20 0 0 0 24 44Z" />
      <path fill="#4CAF50" d="M12.8 27.9a12 12 0 0 1 0-7.8v-5.3H5.9a20 20 0 0 0 0 18.4l6.9-5.3Z" />
      <path fill="#1976D2" d="M24 12c3 0 5.7 1 7.8 3.1l5.8-5.8C34.1 6 29.5 4 24 4A20 20 0 0 0 5.9 14.8l6.9 5.3C14.4 15.5 18.8 12 24 12Z" />
    </svg>
  );
}

function Dashboard({ auth, onSignOut }) {
  const isHotelOwner = auth.role === "HOTEL_OWNER";
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const greetingName = useMemo(
    () => auth.name?.trim().split(/\s+/)[0] || auth.email?.split("@")[0] || "there",
    [auth.email, auth.name],
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");

    const loadHotels = async () => {
      try {
        if (isHotelOwner) {
          const ownedHotels = await apiRequest("/hotels/owned", auth.accessToken);
          const withRatings = await Promise.all(
            ownedHotels.map(async (hotel) => {
              const details = await apiRequest(`/hotels/${encodeURIComponent(hotelId(hotel))}`, auth.accessToken);
              return { hotel, ratings: details.ratings || [] };
            }),
          );
          if (!cancelled) setHotels(withRatings);
        } else {
          const publicHotels = await apiRequest("/hotels", auth.accessToken);
          const withRatings = await Promise.all(
            publicHotels.map(async (hotel) => {
              const details = await apiRequest(`/hotels/${encodeURIComponent(hotelId(hotel))}`, auth.accessToken);
              return { hotel, ratings: details.ratings || [] };
            }),
          );
          if (!cancelled) setHotels(withRatings);
        }
      } catch (requestError) {
        if (!cancelled) setError(requestError.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadHotels();
    return () => { cancelled = true; };
  }, [auth.accessToken, isHotelOwner, refreshKey]);

  const reload = useCallback(() => setRefreshKey((key) => key + 1), []);

  return (
    <main className="dashboard-page">
      <header className="dashboard-topbar">
        <Brand compact />
        <div className="account-menu">
          <span className="account-role">{isHotelOwner ? "HOTEL PARTNER" : "TRAVELLER"}</span>
          <span className="account-name">{greetingName}</span>
          <button className="text-button" type="button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <section className="dashboard-hero">
        <div>
          <span className="eyebrow"><span className="eyebrow-dot" /> {isHotelOwner ? "YOUR PLACE, YOUR STORY" : "A WORLD OF GOOD STAYS"}</span>
          <h1>{isHotelOwner ? <>Welcome back,<br /><em>{greetingName}.</em></> : <>Find a stay<br /><em>to remember.</em></>}</h1>
          <p>{isHotelOwner
            ? "Your hotel, all in one place. Add a property or see what guests have shared."
            : "Browse independent places and discover the details that make a trip."}</p>
        </div>
        <div className="dashboard-stamp" aria-hidden="true">
          <span>STAY</span><strong>✳</strong><span>CURIOUS</span>
        </div>
      </section>

      {isHotelOwner ? (
        <HotelOwnerDashboard
          auth={auth}
          hotels={hotels}
          loading={loading}
          error={error}
          onReload={reload}
        />
      ) : (
        <TravellerDashboard
          auth={auth}
          hotels={hotels}
          loading={loading}
          error={error}
          onReload={reload}
        />
      )}

      <footer className="dashboard-footer">
        <Brand compact />
        <span>Good places. Good people. Better stories.</span>
      </footer>
    </main>
  );
}

function TravellerDashboard({ auth, hotels, loading, error, onReload }) {
  return (
    <section className="listing-section">
      <div className="section-heading">
        <div><span className="eyebrow">THE COLLECTION</span><h2>Places to discover</h2></div>
        <span className="listing-count">{hotels.length.toString().padStart(2, "0")} PLACES</span>
      </div>
      {error && <div className="notice error-notice" role="alert">{error}</div>}
      {loading ? <LoadingState /> : hotels.length ? (
        <div className="hotel-grid">
          {hotels.map(({ hotel, ratings }) => (
            <TravellerHotelCard
              key={hotelId(hotel)}
              hotel={hotel}
              ratings={ratings}
              auth={auth}
              onReload={onReload}
            />
          ))}
        </div>
      ) : <EmptyState title="No stays just yet" detail="New places are finding their way here. Check back soon." />}
    </section>
  );
}

function TravellerHotelCard({ hotel, ratings, auth, onReload }) {
  const [score, setScore] = useState(5);
  const [feedback, setFeedback] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [showReview, setShowReview] = useState(false);
  const id = hotelId(hotel);
  const average = ratings.length
    ? (ratings.reduce((sum, review) => sum + Number(review.rating || 0), 0) / ratings.length).toFixed(1)
    : "—";

  async function submitReview(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await apiRequest("/ratings", auth.accessToken, {
        method: "POST",
        body: JSON.stringify({ hotelId: id, rating: Number(score), feedback: feedback.trim() }),
      });
      setFeedback("");
      setScore(5);
      setShowReview(false);
      onReload();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <article className="hotel-card">
      <div className="hotel-card-art">
        <HotelIllustration seed={hotel.name || id} />
        <span className="art-label">STAYFOLIO SELECT</span>
        <span className="rating-pill">✳ {average} <small>({ratings.length})</small></span>
      </div>
      <div className="hotel-card-body">
        <span className="hotel-location">{hotel.location || "A place to discover"}</span>
        <h3>{hotel.name}</h3>
        <p>{hotel.about || "A welcoming stay with a story of its own."}</p>
        {ratings.length > 0 && (
          <div className="review-list">
            {ratings.slice(0, 2).map((review) => (
              <div className="review-quote" key={review.ratingId}>
                <span aria-label={`${review.rating} out of 5 stars`}>{"★".repeat(Math.max(0, Math.min(5, review.rating)))}</span>
                <p>{review.feedback || "A memorable stay."}</p>
              </div>
            ))}
          </div>
        )}
        <button className="outline-button" type="button" onClick={() => setShowReview((show) => !show)}>
          {showReview ? "Close review" : "Share your review"} <span aria-hidden="true">↗</span>
        </button>
        {showReview && (
          <form className="review-form" onSubmit={submitReview}>
            <label htmlFor={`score-${id}`}>Your rating</label>
            <select id={`score-${id}`} value={score} onChange={(event) => setScore(event.target.value)}>
              {[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} / 5 stars</option>)}
            </select>
            <label htmlFor={`feedback-${id}`}>A few words about your stay</label>
            <textarea id={`feedback-${id}`} value={feedback} onChange={(event) => setFeedback(event.target.value)} maxLength={1000} required rows={3} />
            {error && <p className="error-message" role="alert">{error}</p>}
            <button className="solid-button" type="submit" disabled={submitting}>{submitting ? "Sharing…" : "Post review"}</button>
          </form>
        )}
      </div>
    </article>
  );
}

function HotelOwnerDashboard({ auth, hotels, loading, error, onReload }) {
  const [form, setForm] = useState({ name: "", location: "", about: "" });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");

  async function createHotel(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError("");
    try {
      await apiRequest("/hotels", auth.accessToken, {
        method: "POST",
        body: JSON.stringify(form),
      });
      setForm({ name: "", location: "", about: "" });
      onReload();
    } catch (requestError) {
      setFormError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function removeHotel(id) {
    if (!window.confirm("Remove this hotel listing? This cannot be undone.")) return;
    try {
      await apiRequest(`/hotels/${encodeURIComponent(id)}`, auth.accessToken, { method: "DELETE" });
      onReload();
    } catch (requestError) {
      setFormError(requestError.message);
    }
  }

  return (
    <section className="owner-layout">
      <div className="owner-main">
        <div className="section-heading">
          <div><span className="eyebrow">YOUR COLLECTION</span><h2>Your places</h2></div>
          <span className="listing-count">{hotels.length.toString().padStart(2, "0")} LISTED</span>
        </div>
        {error && <div className="notice error-notice" role="alert">{error}</div>}
        {loading ? <LoadingState /> : hotels.length ? (
          <div className="owner-hotel-list">
            {hotels.map(({ hotel, ratings }) => (
              <article className="owner-hotel-card" key={hotelId(hotel)}>
                <div className="owner-art"><HotelIllustration seed={hotel.name || hotelId(hotel)} /></div>
                <div className="owner-hotel-info">
                  <span className="hotel-location">{hotel.location || "Location not set"}</span>
                  <h3>{hotel.name}</h3>
                  <p>{hotel.about || "No description yet."}</p>
                  <div className="owner-review-summary">✳ {ratings.length} guest {ratings.length === 1 ? "review" : "reviews"}</div>
                  {ratings.slice(0, 3).map((review) => (
                    <blockquote key={review.ratingId}>
                      <span>{"★".repeat(Math.max(0, Math.min(5, review.rating)))}</span>
                      {review.feedback || "A memorable stay."}
                    </blockquote>
                  ))}
                </div>
                <button className="remove-button" type="button" onClick={() => removeHotel(hotelId(hotel))} aria-label={`Remove ${hotel.name}`}>×</button>
              </article>
            ))}
          </div>
        ) : <EmptyState title="Your first place starts here" detail="Add a hotel and make room for your next guest." />}
      </div>

      <aside className="create-panel">
        <span className="eyebrow">NEW LISTING</span>
        <h2>Share your place.</h2>
        <p>A few details are all it takes to get started.</p>
        <form className="create-form" onSubmit={createHotel}>
          <label htmlFor="hotel-name">Hotel name</label>
          <input id="hotel-name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={120} required placeholder="The Corner House" />
          <label htmlFor="hotel-location">Location</label>
          <input id="hotel-location" value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} maxLength={120} required placeholder="Portland, Oregon" />
          <label htmlFor="hotel-about">Tell us about it</label>
          <textarea id="hotel-about" value={form.about} onChange={(event) => setForm({ ...form, about: event.target.value })} maxLength={1000} rows={4} placeholder="What makes your place special?" />
          {formError && <p className="error-message" role="alert">{formError}</p>}
          <button className="solid-button" type="submit" disabled={submitting}>{submitting ? "Creating…" : "Create listing"} <span aria-hidden="true">↗</span></button>
        </form>
        <div className="owner-note"><span aria-hidden="true">✳</span><span>Your listing is only manageable from your hotel-owner account.</span></div>
      </aside>
    </section>
  );
}

function HotelIllustration({ seed }) {
  const palette = ["art-sage", "art-sand", "art-clay", "art-blue"];
  const index = Array.from(seed).reduce((sum, char) => sum + char.charCodeAt(0), 0) % palette.length;
  return (
    <div className={`hotel-illustration ${palette[index]}`} aria-hidden="true">
      <div className="sun" />
      <div className="hill hill-back" />
      <div className="hill hill-front" />
      <div className="building">
        <span className="building-roof" />
        <span className="building-window window-one" />
        <span className="building-window window-two" />
        <span className="building-door" />
      </div>
      <span className="plant plant-one" />
      <span className="plant plant-two" />
    </div>
  );
}

function LoadingState() {
  return <div className="loading-state"><span className="spinner" /> Gathering the good places…</div>;
}

function EmptyState({ title, detail }) {
  return (
    <div className="empty-state">
      <span aria-hidden="true">✳</span>
      <h3>{title}</h3>
      <p>{detail}</p>
    </div>
  );
}

export default App;
