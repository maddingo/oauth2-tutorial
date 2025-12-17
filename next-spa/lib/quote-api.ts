
export interface Quote {
    quote:  string;
    author: string;
}

export async function getQuote(accessToken :string): Promise<Quote> {
  const response = await fetch('https://api.quotable.io/random', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch quote');
  }
    return await response.json();
}
