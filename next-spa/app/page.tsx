'use client';

import { useAuth } from '@/context/AuthContext';

export default function Home() {
  const { isAuthenticated, userInfo, login, logout } = useAuth();

  return (
    <main className="container">
      <h1 className="title">OAuth2.1 PKCE Demo</h1>
      <p className="subtitle">Next.js SPA with Authorization Code Flow + PKCE</p>

      {!isAuthenticated ? (
        <div>
          <p style={{ marginBottom: '1.5rem', color: '#64748b' }}>
            Click the button below to authenticate using OAuth2.1 with PKCE.
          </p>
          <button className="button" onClick={login}>
            Sign In
          </button>
        </div>
      ) : (
        <div>
          <div className="user-info">
            <h3>User Information</h3>
            {userInfo && (
              <>
                <div className="info-item">
                  <span className="info-label">Subject:</span>
                  <span className="info-value">{userInfo.sub}</span>
                </div>
                {userInfo.name && (
                  <div className="info-item">
                    <span className="info-label">Name:</span>
                    <span className="info-value">{userInfo.name}</span>
                  </div>
                )}
                {userInfo.email && (
                  <div className="info-item">
                    <span className="info-label">Email:</span>
                    <span className="info-value">{userInfo.email}</span>
                  </div>
                )}
              </>
            )}
          </div>
          <button className="button secondary" onClick={logout}>
            Sign Out
          </button>
        </div>
      )}
    </main>
  );
}
