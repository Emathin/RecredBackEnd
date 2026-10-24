const { CognitoIdentityProviderClient, CreateGroupCommand, AdminAddUserToGroupCommand } = require('@aws-sdk/client-cognito-identity-provider');

const client = new CognitoIdentityProviderClient({
  region: 'us-east-2',
  credentials: {
    accessKeyId: 'AKIAQH66KSFLZ4PZ7QPP',
    secretAccessKey: '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'
  }
});

const userPoolId = 'us-east-2_Qzg5XbEUj';

async function addKiosqueroGroup() {
  try {
    try {
      await client.send(new CreateGroupCommand({
        UserPoolId: userPoolId,
        GroupName: 'Kiosqueros',
        Description: 'Grupo para Kiosqueros / Vendedores'
      }));
      console.log('Group Kiosqueros created');
    } catch (e) {
      console.log('Group already exists or error:', e.message);
    }

    await client.send(new AdminAddUserToGroupCommand({
      UserPoolId: userPoolId,
      Username: 'alexis',
      GroupName: 'Kiosqueros'
    }));
    console.log('Added alexis to group Kiosqueros successfully');
  } catch (err) {
    console.error('Error:', err);
  }
}

addKiosqueroGroup();
