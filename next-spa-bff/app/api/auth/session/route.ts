import { NextRequest, NextResponse } from 'next/server';
import { getAccessToken, getCookie, isTokenExpired } from '@/lib/cookies';

/**
 * API route to get current session/authentication status
 * GET /api/auth/session
 *
 * This route:
 * 1. Checks if user is authenticated (has valid access token)
 * 2. Returns user info if authenticated
 * 3. Returns authentication status
 */
export async function GET(request: NextRequest) {
  try {
    // Check if access token exists
    const accessToken = await getAccessToken();

    if (!accessToken) {
      return NextResponse.json(
        {
          isAuthenticated: false,
          user: null,
        },
        { status: 200 }
      );
    }

    // Check if token is expired
    const expired = await isTokenExpired();

    if (expired) {
      return NextResponse.json(
        {
          isAuthenticated: false,
          user: null,
          message: 'Token expired',
        },
        { status: 200 }
      );
    }

    // Get user info from cookie
    const userInfoStr = await getCookie('user_info');
    let userInfo = null;

    if (userInfoStr) {
      try {
        userInfo = JSON.parse(userInfoStr);
      } catch (e) {
        console.error('Failed to parse user info:', e);
      }
    }

    return NextResponse.json(
      {
        isAuthenticated: true,
        user: userInfo,
      },
      { status: 200 }
    );
  } catch (error) {
    console.error('Session check error:', error);
    return NextResponse.json(
      {
        isAuthenticated: false,
        user: null,
        error: 'Failed to check session',
      },
      { status: 500 }
    );
  }
}
