import { NextRequest, NextResponse } from 'next/server';
import { oauthConfig } from '@/config/oauth';
import { getCookie, deleteCookie, setAuthCookies, setSecureCookie } from '@/lib/cookies';

/**
 * API route to handle OAuth2 callback
 * GET /api/auth/callback?code=...&state=...
 *
 * This route:
 * 1. Validates state parameter (CSRF protection)
 * 2. Retrieves code_verifier from secure cookie
 * 3. Exchanges authorization code for tokens
 * 4. Fetches user info
 * 5. Stores tokens in secure HttpOnly cookies
 * 6. Redirects to home page
 */
export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    const errorParam = searchParams.get('error');
    const errorDescription = searchParams.get('error_description');

    console.log('OAuth2 Callback received via API route:', {
      hasCode: !!code,
      hasState: !!state,
      hasError: !!errorParam,
    });

    // Check for authorization errors
    if (errorParam) {
      const errorMsg = encodeURIComponent(errorDescription || errorParam);
      return NextResponse.redirect(
        new URL(`/?error=${errorMsg}`, request.url)
      );
    }

    // Validate required parameters
    if (!code) {
      return NextResponse.redirect(
        new URL('/?error=Authorization code not found', request.url)
      );
    }

    if (!state) {
      return NextResponse.redirect(
        new URL('/?error=State parameter not found', request.url)
      );
    }

    // Retrieve stored state and code_verifier from cookies
    const storedState = await getCookie('oauth_state');
    const codeVerifier = await getCookie('code_verifier');

    console.log('Cookie validation:', {
      hasStoredState: !!storedState,
      hasCodeVerifier: !!codeVerifier,
      receivedState: state,
      storedState: storedState,
    });

    // Validate state (CSRF protection)
    if (!storedState) {
      return NextResponse.redirect(
        new URL('/?error=No stored state found - session may have expired', request.url)
      );
    }

    if (storedState !== state) {
      console.error('State mismatch:', { received: state, stored: storedState });
      return NextResponse.redirect(
        new URL('/?error=Invalid state parameter - possible CSRF attack', request.url)
      );
    }

    // Validate code_verifier
    if (!codeVerifier) {
      return NextResponse.redirect(
        new URL('/?error=Code verifier not found - session may have expired', request.url)
      );
    }

    console.log('State validation successful, exchanging code for token...');

    // Exchange authorization code for tokens
    const tokenParams = new URLSearchParams({
      grant_type: 'authorization_code',
      code: code,
      redirect_uri: oauthConfig.redirectUri,
      client_id: oauthConfig.clientId,
      code_verifier: codeVerifier,
    });

    const tokenResponse = await fetch(oauthConfig.tokenEndpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: tokenParams.toString(),
    });

    if (!tokenResponse.ok) {
      const error = await tokenResponse.text();
      console.error('Token exchange failed:', error);
      return NextResponse.redirect(
        new URL(`/?error=Token exchange failed: ${encodeURIComponent(error)}`, request.url)
      );
    }

    const tokens = await tokenResponse.json();
    console.log('Token exchange successful:', {
      expiresIn: tokens.expires_in,
      hasRefreshToken: !!tokens.refresh_token
    });

    // Fetch user info
    const userInfoResponse = await fetch(oauthConfig.userinfoEndpoint, {
      headers: {
        Authorization: `Bearer ${tokens.access_token}`,
      },
    });

    if (!userInfoResponse.ok) {
      console.error('Failed to fetch user info');
      return NextResponse.redirect(
        new URL('/?error=Failed to fetch user info', request.url)
      );
    }

    const userInfo = await userInfoResponse.json();
    console.log('User info fetched successfully:', { sub: userInfo.sub });

    // Store tokens in secure HttpOnly cookies
    await setAuthCookies(tokens);

    // Store user info in a cookie (not HttpOnly since we need to read it client-side)
    await setSecureCookie('user_info', JSON.stringify(userInfo), {
      httpOnly: false, // Allow client-side access for display
      maxAge: tokens.expires_in,
    });

    // Clean up temporary cookies
    await deleteCookie('code_verifier');
    await deleteCookie('oauth_state');

    console.log('Authentication successful, redirecting to home page');

    // Redirect to home page
    return NextResponse.redirect(new URL('/', request.url));
  } catch (error) {
    console.error('OAuth callback error:', error);
    const errorMsg = error instanceof Error ? error.message : 'Authentication failed';
    return NextResponse.redirect(
      new URL(`/?error=${encodeURIComponent(errorMsg)}`, request.url)
    );
  }
}
