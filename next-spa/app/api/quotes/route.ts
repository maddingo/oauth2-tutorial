import { NextRequest, NextResponse } from 'next/server';
import { getAccessToken, isTokenExpired } from '@/lib/cookies';

const RESOURCE_SERVER_URL = process.env.RESOURCE_SERVER_URL || 'http://localhost:8090';

/**
 * API proxy route for quotes from resource server
 * GET /api/quotes
 *
 * This route:
 * 1. Retrieves access token from secure HttpOnly cookie
 * 2. Checks if token is expired
 * 3. Proxies request to resource server with Bearer token
 * 4. Returns quote data to client
 *
 * This implements the BFF pattern - frontend never sees the access token
 */
export async function GET(request: NextRequest) {
  try {
    // Get access token from cookie
    const accessToken = await getAccessToken();

    if (!accessToken) {
      return NextResponse.json(
        { error: 'Not authenticated' },
        { status: 401 }
      );
    }

    // Check if token is expired
    const expired = await isTokenExpired();

    if (expired) {
      return NextResponse.json(
        { error: 'Token expired', needsRefresh: true },
        { status: 401 }
      );
    }

    // Proxy request to resource server
    const response = await fetch(`${RESOURCE_SERVER_URL}/quote`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.text();
      console.error('Resource server error:', error);

      // Check if it's an authentication error
      if (response.status === 401) {
        return NextResponse.json(
          { error: 'Invalid or expired token', needsRefresh: true },
          { status: 401 }
        );
      }

      return NextResponse.json(
        { error: 'Failed to fetch quote' },
        { status: response.status }
      );
    }

    const quote = await response.json();

    return NextResponse.json(quote, { status: 200 });
  } catch (error) {
    console.error('Quote proxy error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch quote' },
      { status: 500 }
    );
  }
}
