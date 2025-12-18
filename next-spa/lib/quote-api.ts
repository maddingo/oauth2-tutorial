import { authenticatedFetch } from './authenticatedFetch';

export interface Quote {
    quote:  string;
    author: string;
}

/**
 * Fetch a quote from the resource server with automatic token refresh
 */
export async function getQuote(): Promise<Quote> {
  const response = await authenticatedFetch('http://localhost:8090/quote', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    }
  });

  if (!response.ok) {
    const error = await response.text();
    console.error('Failed to fetch quote:', error);
    throw new Error('Failed to fetch quote');
  }

  return await response.json();
}
