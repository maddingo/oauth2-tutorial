import { oauthConfig } from '@/config/oauth';
import { generateCodeChallenge, generateCodeVerifier, generateState } from './pkce';

export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token?: string;
  scope: string;
  id_token?: string;
}

export interface UserInfo {
  sub: string;
  name?: string;
  email?: string;
  [key: string]: any;
}

/**
 * Initiate OAuth2 authorization flow with PKCE
 */
export async function initiateLogin(): Promise<void> {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  const state = generateState();

  // Store verifier and state in sessionStorage for callback
  sessionStorage.setItem('code_verifier', codeVerifier);
  sessionStorage.setItem('oauth_state', state);

  console.log('OAuth2 Login initiated:', {
    state,
    codeVerifier: codeVerifier.substring(0, 10) + '...',
    codeChallenge: codeChallenge.substring(0, 10) + '...',
  });

  const params = new URLSearchParams({
    response_type: oauthConfig.responseType,
    client_id: oauthConfig.clientId,
    redirect_uri: oauthConfig.redirectUri,
    scope: oauthConfig.scope,
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
  });

  window.location.href = `${oauthConfig.authorizationEndpoint}?${params.toString()}`;
}

/**
 * Exchange authorization code for tokens
 */
export async function exchangeCodeForToken(code: string): Promise<TokenResponse> {
  const codeVerifier = sessionStorage.getItem('code_verifier');

  if (!codeVerifier) {
    throw new Error('Code verifier not found in session');
  }

  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    code: code,
    redirect_uri: oauthConfig.redirectUri,
    client_id: oauthConfig.clientId,
    code_verifier: codeVerifier,
  });

  const response = await fetch(oauthConfig.tokenEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Token exchange failed: ${error}`);
  }

  const tokens: TokenResponse = await response.json();

  // Clean up stored verifier
  sessionStorage.removeItem('code_verifier');
  sessionStorage.removeItem('oauth_state');

  return tokens;
}

/**
 * Fetch user info using access token
 */
export async function fetchUserInfo(accessToken: string): Promise<UserInfo> {
  const response = await fetch(oauthConfig.userinfoEndpoint, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch user info');
  }

  return response.json();
}

/**
 * Validate state parameter for CSRF protection
 */
export function validateState(receivedState: string): boolean {
  const storedState = sessionStorage.getItem('oauth_state');
  return storedState === receivedState;
}

/**
 * Refresh access token using refresh token
 */
export async function refreshAccessToken(refreshToken: string): Promise<TokenResponse> {
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: oauthConfig.clientId,
  });

  const response = await fetch(oauthConfig.tokenEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Token refresh failed: ${error}`);
  }

  return response.json();
}
