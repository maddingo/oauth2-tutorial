import { NextRequest, NextResponse } from 'next/server';
import { oauthConfig } from '@/config/oauth';
import { getRefreshToken, setAuthCookies, clearAuthCookies } from '@/lib/cookies';

/**
 * API route to refresh access token
 * POST /api/auth/refresh
 *
 * This route:
 * 1. Retrieves refresh token from secure HttpOnly cookie
 * 2. Exchanges refresh token for new access token
 * 3. Updates cookies with new tokens
 * 4. Returns success response
 */
export async function POST(request: NextRequest) {
  try {
    // Get refresh token from cookie
    const refreshToken = await getRefreshToken();

    if (!refreshToken) {
      return NextResponse.json(
        { error: 'No refresh token available' },
        { status: 401 }
      );
    }

    console.log('Attempting to refresh access token...');

    // Exchange refresh token for new access token
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
      console.error('Token refresh failed:', error);

      // Clear cookies if refresh fails (token might be expired/revoked)
      await clearAuthCookies();

      return NextResponse.json(
        { error: 'Token refresh failed', details: error },
        { status: 401 }
      );
    }

    const tokens = await response.json();
    console.log('Token refreshed successfully:', {
      expiresIn: tokens.expires_in,
      hasNewRefreshToken: !!tokens.refresh_token,
    });

    // Update cookies with new tokens
    await setAuthCookies(tokens);

    return NextResponse.json(
      { message: 'Token refreshed successfully' },
      { status: 200 }
    );
  } catch (error) {
    console.error('Token refresh error:', error);
    await clearAuthCookies();

    return NextResponse.json(
      { error: 'Failed to refresh token' },
      { status: 500 }
    );
  }
}
