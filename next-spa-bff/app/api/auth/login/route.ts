import { NextRequest, NextResponse } from 'next/server';
import { generateCodeChallenge, generateCodeVerifier, generateState } from '@/lib/pkce';
import { oauthConfig } from '@/config/oauth';
import { setSecureCookie } from '@/lib/cookies';

/**
 * API route to initiate OAuth2 login with PKCE
 * GET /api/auth/login
 *
 * This route:
 * 1. Generates PKCE code_verifier and code_challenge
 * 2. Generates state for CSRF protection
 * 3. Stores verifier and state in secure HttpOnly cookies
 * 4. Redirects to authorization server
 */
export async function GET(request: NextRequest) {
  try {
    // Generate PKCE parameters
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    const state = generateState();

    // Store verifier and state in secure HttpOnly cookies
    // These will be used during the callback
    await setSecureCookie('code_verifier', codeVerifier, {
      maxAge: 60 * 10, // 10 minutes
    });

    await setSecureCookie('oauth_state', state, {
      maxAge: 60 * 10, // 10 minutes
    });

    console.log('OAuth2 Login initiated via API route:', {
      state,
      codeVerifier: codeVerifier.substring(0, 10) + '...',
      codeChallenge: codeChallenge.substring(0, 10) + '...',
    });

    // Build authorization URL
    const params = new URLSearchParams({
      response_type: oauthConfig.responseType,
      client_id: oauthConfig.clientId,
      redirect_uri: oauthConfig.redirectUri,
      scope: oauthConfig.scope,
      state: state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });

    const authUrl = `${oauthConfig.authorizationEndpoint}?${params.toString()}`;

    // Redirect to authorization server
    return NextResponse.redirect(authUrl);
  } catch (error) {
    console.error('Login initiation error:', error);
    return NextResponse.json(
      { error: 'Failed to initiate login' },
      { status: 500 }
    );
  }
}
