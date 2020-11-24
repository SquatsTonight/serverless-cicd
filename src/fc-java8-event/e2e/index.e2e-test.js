var FCClient = require('@alicloud/fc2');

describe('invoke function', () => {
    const fcCli = new FCClient(process.env.ACCOUNT_ID, {
      accessKeyID: process.env.ACCESS_KEY_ID,
      accessKeySecret: process.env.ACCESS_KEY_SECRET,
      region: process.env.REGION,
    });
    const targetService = process.env.SERVICE;
    const targetFunction = process.env.FUNCTION;

    it('invoke success', async() => {
        resp = await fcCli.invokeFunction(
          targetService, 
          targetFunction, 
          JSON.stringify({
            "firstName": "FC",
            "lastName": "aliyun"
          })
        );
        expect(resp.data).toContain('Hello, FC aliyun');
    });
});