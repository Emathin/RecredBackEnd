const { CognitoIdentityProviderClient, AdminGetUserCommand, AdminListGroupsForUserCommand, AdminAddUserToGroupCommand, ListGroupsCommand } = require('@aws-sdk/client-cognito-identity-provider');

const client = new CognitoIdentityProviderClient({
  region: 'us-east-2',
  credentials: {
    accessKeyId: 'AKIAQH66KSFLZ4PZ7QPP',
    secretAccessKey: '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'
  }
});

const userPoolId = 'us-east-2_Qzg5XbEUj';
const username = 'ying_ib@hotmail.com';

async function check() {
  try {
    const userRes = await client.send(new AdminGetUserCommand({
      UserPoolId: userPoolId,
      Username: username
    }));
    console.log('User Status:', userRes.UserStatus);
    console.log('Enabled:', userRes.Enabled);
    console.log('Attributes:', userRes.UserAttributes.map(a => `${a.Name}=${a.Value}`).join(', '));

    const groupsRes = await client.send(new AdminListGroupsForUserCommand({
      UserPoolId: userPoolId,
      Username: username
    }));
    console.log('User Groups:', groupsRes.Groups.map(g => g.GroupName));

    const allGroups = await client.send(new ListGroupsCommand({
      UserPoolId: userPoolId
    }));
    console.log('All Pool Groups:', allGroups.Groups.map(g => g.GroupName));
  } catch (err) {
    console.error('Cognito Error:', err.message);
  }
}

check();
