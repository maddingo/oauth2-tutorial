export const oauthConfig = {
  authorizationEndpoint: 'http://idp:9000/oauth2/authorize',
  tokenEndpoint: 'http://idp:9000/oauth2/token',
  userinfoEndpoint: 'http://idp:9000/userinfo',
  clientId: 'public-client',
  redirectUri: 'http://localhost:3000/callback',
  scope: 'openid profile',
  responseType: 'code',
};
