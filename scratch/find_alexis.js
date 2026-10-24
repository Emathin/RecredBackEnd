const { CognitoIdentityProviderClient, ListUsersCommand } = require('@aws-sdk/client-cognito-identity-provider');
const client = new CognitoIdentityProviderClient({
  region: 'us-east-2',
  credentials: {
    accessKeyId: 'AKIAQH66KSFLZ4PZ7QPP',
    secretAccessKey: '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'
  }
});

async function run() {
  let paginationToken = undefined;
  do {
    const res = await client.send(new ListUsersCommand({ 
      UserPoolId: 'us-east-2_Qzg5XbEUj',
      PaginationToken: paginationToken
    }));
    for (const u of res.Users) {
      const email = u.Attributes.find(a => a.Name === 'email')?.Value;
      if (email && email.toLowerCase().includes('ying_ib') || u.Username.toLowerCase().includes('alexis')) {
        const sub = u.Attributes.find(a => a.Name === 'sub')?.Value;
        console.log('Username:', u.Username, '| Email:', email, '| Sub:', sub, '| Status:', u.UserStatus, '| Enabled:', u.Enabled);
      }
    }
    paginationToken = res.PaginationToken;
  } while (paginationToken);
}

run().catch(console.error);
