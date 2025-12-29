import { cookies } from 'next/headers';

export interface CookieOptions {
  httpOnly?: boolean;
  secure?: boolean;
  sameSite?: 'lax' | 'strict' | 'none';
  maxAge?: number;
  path?: string;
}

const isProduction = process.env.NODE_ENV === 'production';

/**
 * Default secure cookie options for production
 */
export const SECURE_COOKIE_OPTIONS: CookieOptions = {
  httpOnly: true,
  secure: isProduction, // Only use secure flag in production (requires HTTPS)
  sameSite: 'lax',
  path: '/',
};

/**
 * Cookie names for token storage
 */
export const COOKIE_NAMES = {
  ACCESS_TOKEN: 'access_token',
  REFRESH_TOKEN: 'refresh_token',
  TOKEN_EXPIRATION: 'token_expiration',
  USER_INFO: 'user_info',
} as const;

/**
 * Set a secure cookie
 */
export async function setSecureCookie(
  name: string,
  value: string,
  options: CookieOptions = {}
): Promise<void> {
  const cookieStore = await cookies();
  const mergedOptions = { ...SECURE_COOKIE_OPTIONS, ...options };

  cookieStore.set(name, value, mergedOptions);
}

/**
 * Get a cookie value
 */
export async function getCookie(name: string): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(name)?.value;
}

/**
 * Delete a cookie
 */
export async function deleteCookie(name: string): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(name);
}

/**
 * Set all authentication cookies
 */
export async function setAuthCookies(tokens: {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
}): Promise<void> {
  // Set access token (short-lived)
  await setSecureCookie(COOKIE_NAMES.ACCESS_TOKEN, tokens.access_token, {
    maxAge: tokens.expires_in,
  });

  // Set refresh token (longer-lived) if present
  if (tokens.refresh_token) {
    await setSecureCookie(COOKIE_NAMES.REFRESH_TOKEN, tokens.refresh_token, {
      maxAge: 60 * 60 * 24 * 7, // 7 days
    });
  }

  // Set token expiration timestamp
  const expirationTime = Date.now() + tokens.expires_in * 1000;
  await setSecureCookie(COOKIE_NAMES.TOKEN_EXPIRATION, expirationTime.toString(), {
    maxAge: tokens.expires_in,
  });
}

/**
 * Clear all authentication cookies
 */
export async function clearAuthCookies(): Promise<void> {
  await deleteCookie(COOKIE_NAMES.ACCESS_TOKEN);
  await deleteCookie(COOKIE_NAMES.REFRESH_TOKEN);
  await deleteCookie(COOKIE_NAMES.TOKEN_EXPIRATION);
  await deleteCookie(COOKIE_NAMES.USER_INFO);
}

/**
 * Get access token from cookies
 */
export async function getAccessToken(): Promise<string | undefined> {
  return getCookie(COOKIE_NAMES.ACCESS_TOKEN);
}

/**
 * Get refresh token from cookies
 */
export async function getRefreshToken(): Promise<string | undefined> {
  return getCookie(COOKIE_NAMES.REFRESH_TOKEN);
}

/**
 * Check if token is expired
 */
export async function isTokenExpired(): Promise<boolean> {
  const expirationStr = await getCookie(COOKIE_NAMES.TOKEN_EXPIRATION);

  if (!expirationStr) {
    return true;
  }

  const expiration = parseInt(expirationStr, 10);
  // Add 60 second buffer to prevent mid-request expiration
  return Date.now() >= expiration - 60000;
}
