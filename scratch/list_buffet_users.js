const { CognitoIdentityProviderClient, ListUsersInGroupCommand } = require('@aws-sdk/client-cognito-identity-provider');

const client = new CognitoIdentityProviderClient({
  region: 'us-east-2',
  credentials: {
    accessKeyId: 'AKIAQH66KSFLZ4PZ7QPP',
    secretAccessKey: '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'
  }
});

const userPoolId = 'us-east-2_Qzg5XbEUj';

async function listBuffetUsers() {
  try {
    const res = await client.send(new ListUsersInGroupCommand({
      UserPoolId: userPoolId,
      GroupName: 'Buffet'
    }));
    console.log('Users in Buffet group:');
    for (const u of res.Users) {
      const email = u.Attributes.find(a => a.Name === 'email')?.Value;
      console.log(`- Username: ${u.Username}, Email: ${email}, Status: ${u.UserStatus}`);
    }
  } catch (err) {
    console.error('Error:', err.message);
  }
}

listBuffetUsers();
