
export interface Quote {
    quote:  string;
    author: string;
}

export async function getQuote(accessToken :string): Promise<Quote> {
  const response = await fetch('http://localhost:8090/quote', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch quote: ' + response.text);
  }
    return await response.json();
}
