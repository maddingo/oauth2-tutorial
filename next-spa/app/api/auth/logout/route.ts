import { NextRequest, NextResponse } from 'next/server';
import { clearAuthCookies } from '@/lib/cookies';

/**
 * API route to handle logout
 * POST /api/auth/logout
 *
 * This route:
 * 1. Clears all authentication cookies
 * 2. Returns success response
 */
export async function POST(request: NextRequest) {
  try {
    // Clear all authentication cookies
    await clearAuthCookies();

    console.log('User logged out successfully');

    return NextResponse.json(
      { message: 'Logged out successfully' },
      { status: 200 }
    );
  } catch (error) {
    console.error('Logout error:', error);
    return NextResponse.json(
      { error: 'Failed to logout' },
      { status: 500 }
    );
  }
}

/**
 * Alternative: GET endpoint for simple redirect-based logout
 */
export async function GET(request: NextRequest) {
  try {
    await clearAuthCookies();
    console.log('User logged out successfully');

    // Redirect to home page
    return NextResponse.redirect(new URL('/', request.url));
  } catch (error) {
    console.error('Logout error:', error);
    return NextResponse.redirect(new URL('/?error=Logout failed', request.url));
  }
}
