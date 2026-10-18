import { useEffect, useState } from "react";
import api from "../services/api";

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
        <p>Loading user profile...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="profile-container p-6">
        <div className="alert alert-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="profile-page p-6 max-w-2xl mx-auto">
      <header className="page-header mb-6">
        <h1 className="text-2xl font-bold">የተጠቃሚ መገለጫ (User Profile)</h1>
        <p className="text-gray-600">Authenticated family member account details.</p>
      </header>

      {profile && (
        <div className="card bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <div className="flex items-center space-x-4 mb-6">
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center text-xl font-bold">
              {profile.full_name ? profile.full_name.charAt(0).toUpperCase() : "U"}
            </div>
            <div>
              <h2 className="text-xl font-bold">{profile.full_name}</h2>
              <span className="inline-block px-3 py-1 text-xs rounded-full bg-emerald-50 text-emerald-700 font-medium">
                {profile.is_active ? "ንቁ አባል (Active Member)" : "Inactive Account"}
              </span>
            </div>
          </div>

          <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="p-3 bg-gray-50 rounded">
              <dt className="text-xs text-gray-500 font-medium">1. Full Name / ስም</dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1">{profile.full_name || "አልተገለጸም"}</dd>
            </div>

            <div className="p-3 bg-gray-50 rounded">
              <dt className="text-xs text-gray-500 font-medium">2. Phone Number / ስልክ ቁጥር</dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1">{profile.phone || "Not provided (አልተገለጸም)"}</dd>
            </div>

            <div className="p-3 bg-gray-50 rounded">
              <dt className="text-xs text-gray-500 font-medium">3. Google Account Email / ኢሜይል</dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1">{profile.email || "Not provided (አልተገለጸም)"}</dd>
            </div>

            <div className="p-3 bg-gray-50 rounded">
              <dt className="text-xs text-gray-500 font-medium">4. Username / የተጠቃሚ ስም</dt>
              <dd className="text-sm font-semibold text-gray-900 mt-1">{profile.username}</dd>
            </div>

            <div className="p-3 bg-gray-50 rounded sm:col-span-2">
              <dt className="text-xs text-gray-500 font-medium">Account ID / መለያ</dt>
              <dd className="text-sm font-mono text-gray-700 mt-1">FM-{profile.id}</dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  );
}

export default Profile;
