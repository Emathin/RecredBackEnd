const { CognitoIdentityProviderClient, AdminSetUserPasswordCommand, AdminInitiateAuthCommand } = require('@aws-sdk/client-cognito-identity-provider');

const client = new CognitoIdentityProviderClient({
  region: 'us-east-2',
  credentials: {
    accessKeyId: 'AKIAQH66KSFLZ4PZ7QPP',
    secretAccessKey: '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'
  }
});

const userPoolId = 'us-east-2_Qzg5XbEUj';
// Let's get the client id from Frontend environment
const fs = require('fs');

async function test() {
  const newPassword = 'Password123!';
  console.log('Setting password for user alexis...');
  await client.send(new AdminSetUserPasswordCommand({
    UserPoolId: userPoolId,
    Username: 'alexis',
    Password: newPassword,
    Permanent: true
  }));
  console.log('Password set successfully!');

  // Read clientId from frontend environment
  const envContent = fs.readFileSync('c:/Users/emman/OneDrive/Desktop/Final TPI/RecredFrontEnd/src/environments/environment.ts', 'utf8');
  const clientIdMatch = envContent.match(/userPoolClientId:\s*'([^']+)'/);
  const clientId = clientIdMatch ? clientIdMatch[1] : null;
  console.log('Frontend ClientId:', clientId);

  if (clientId) {
    try {
      console.log('Attempting authentication with Username: "alexis" ...');
      const auth1 = await client.send(new AdminInitiateAuthCommand({
        UserPoolId: userPoolId,
        ClientId: clientId,
        AuthFlow: 'ADMIN_NO_SRP_AUTH',
        AuthParameters: {
          USERNAME: 'alexis',
          PASSWORD: newPassword
        }
      }));
      console.log('Auth with "alexis": SUCCESS! Token received.');
    } catch (e) {
      console.log('Auth with "alexis" error:', e.message);
    }

    try {
      console.log('Attempting authentication with Username: "ying_ib@hotmail.com" ...');
      const auth2 = await client.send(new AdminInitiateAuthCommand({
        UserPoolId: userPoolId,
        ClientId: clientId,
        AuthFlow: 'ADMIN_NO_SRP_AUTH',
        AuthParameters: {
          USERNAME: 'ying_ib@hotmail.com',
          PASSWORD: newPassword
        }
      }));
      console.log('Auth with "ying_ib@hotmail.com": SUCCESS! Token received.');
    } catch (e) {
      console.log('Auth with "ying_ib@hotmail.com" error:', e.message);
    }
  }
}

test().catch(console.error);
