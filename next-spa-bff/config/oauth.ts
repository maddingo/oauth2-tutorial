export const oauthConfig = {
  authorizationEndpoint: 'http://idp:9000/oauth2/authorize',
  tokenEndpoint: 'http://idp:9000/oauth2/token',
  userinfoEndpoint: 'http://idp:9000/userinfo',
  clientId: 'public-client',
  redirectUri: 'http://localhost:3000/api/auth/callback', // Changed to API route for BFF pattern
  scope: 'openid profile message.read',
  responseType: 'code',
};
