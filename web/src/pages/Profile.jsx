import { useEffect, useState } from "react";
import api from "../services/api";
import PageHeader from "../components/PageHeader";
import "./Profile.css";

function Profile() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function fetchProfile() {
      try {
        setLoading(true);
        const response = await api.get("/auth/me");
        if (response.data?.success && response.data?.data) {
          setProfile(response.data.data);
          // Update cached user in localStorage
          localStorage.setItem("user", JSON.stringify(response.data.data));
        } else {
          throw new Error("Failed to load user profile");
        }
      } catch (err) {
        console.error("Profile load error:", err);
        setError(
          err.response?.data?.message || "Failed to load authenticated profile."
        );
      } finally {
        setLoading(false);
      }
    }

    fetchProfile();
  }, []);

  if (loading) {
    return (
      <div className="profile-container p-6">
        <div className="profile-loading-card">
          <div className="profile-spinner" aria-hidden="true" />
          <p>Loading user profile...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="profile-container p-6">
        <div className="alert alert-error profile-alert-error" role="alert">
          <svg
            className="profile-field-icon"
            style={{ color: "#DC4545" }}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{error}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-page p-6 max-w-2xl mx-auto">
      <PageHeader
        eyebrow="Account Details"
        title="Profile"
        description="Manage your personal information and account settings."
        theme="profile"
      />

      {profile && (
        <div className="card profile-card bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <div className="flex items-center space-x-4 mb-6 profile-card-header">
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center text-xl font-bold profile-avatar profile-hero-avatar">
              {profile.full_name ? profile.full_name.charAt(0).toUpperCase() : "U"}
            </div>
            <div className="profile-identity">
              <h2 className="text-xl font-bold">{profile.full_name}</h2>
              <span
                className={`profile-status-badge inline-block px-3 py-1 text-xs rounded-full font-medium ${
                  profile.is_active
                    ? "status-active bg-emerald-50 text-emerald-700"
                    : "status-inactive"
                }`}
              >
                <span className="profile-status-dot" aria-hidden="true" />
                {profile.is_active ? "Active Member" : "Inactive Account"}
              </span>
            </div>
          </div>

          <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 profile-details-grid">
            <div className="p-3 bg-gray-50 rounded profile-field-item">
              <dt className="text-xs text-gray-500 font-medium profile-field-label">
                <svg
                  className="profile-field-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                1. Full Name
              </dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1 profile-field-value">
                {profile.full_name || "Not provided"}
              </dd>
            </div>

            <div className="p-3 bg-gray-50 rounded profile-field-item">
              <dt className="text-xs text-gray-500 font-medium profile-field-label">
                <svg
                  className="profile-field-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
                2. Phone Number
              </dt>
              <dd
                className={`text-sm font-semibold text-gray-900 mt-1 profile-field-value ${
                  !profile.phone ? "is-empty" : ""
                }`}
              >
                {profile.phone || "Not provided"}
              </dd>
            </div>

            <div className="p-3 bg-gray-50 rounded profile-field-item">
              <dt className="text-xs text-gray-500 font-medium profile-field-label">
                <svg
                  className="profile-field-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
                3. Google Account Email
              </dt>
              <dd
                className={`text-sm font-semibold text-gray-900 mt-1 profile-field-value ${
                  !profile.email ? "is-empty" : ""
                }`}
              >
                {profile.email || "Not provided"}
              </dd>
            </div>

            <div className="p-3 bg-gray-50 rounded profile-field-item">
              <dt className="text-xs text-gray-500 font-medium profile-field-label">
                <svg
                  className="profile-field-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="4" />
                  <path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94" />
                </svg>
                4. Username
              </dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1 profile-field-value">
                {profile.username}
              </dd>
            </div>

            <div className="p-3 bg-gray-50 rounded sm:col-span-2 profile-field-item profile-field-full">
              <dt className="text-xs text-gray-500 font-medium profile-field-label">
                <svg
                  className="profile-field-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                Account ID
              </dt>
              <dd className="text-sm font-mono text-gray-700 mt-1 profile-field-value">
                <span className="profile-field-id-badge">FM-{profile.id}</span>
              </dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  );
}

export default Profile;
