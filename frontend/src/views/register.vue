<template>
  <div class="login-container">
    <div class="login-main">
      <img src="@/assets/logo.png" alt="" style="width: 250px;margin-right:160px;  height: auto;" />
      <el-card style="background-color: #e8faf2; padding: 10px">
        <span style="font-size: 20px;font-weight:bold;color: #3d5dd7;margin: 90px">欢迎注册职场云☁！</span>
        <el-form ref="formRef" :model="data.form" style="margin-top: 20px;" :rules="data.rules">
          <el-form-item label="账号:" prop="userName">
            <el-input placeholder="请输入账号" v-model="data.form.userName" style="width: 290px" :prefix-icon="User"></el-input>
          </el-form-item>
          <el-form-item label="密码:" prop="password">
            <el-input placeholder="请输入密码" v-model="data.form.password" style="width: 290px" show-password :prefix-icon="Lock"></el-input>
          </el-form-item>
          <el-form-item label="确认密码:" prop="confirmPassword" style="margin-right: 11px">
            <el-input placeholder="请输入确认密码" v-model="data.form.confirmPassword" style="width: 272px" :prefix-icon="Lock" show-password></el-input>
          </el-form-item>
          <el-form-item label="请输入验证码:" prop="" style="display: flex; align-items: center;">
            <el-input placeholder="验证码" style="width: 120px;" v-model="data.form.captcha"></el-input>
            <img :src="data.captchaUrl" alt="验证码" style="width: 120px; height: 40px; margin-left: 10px; cursor: pointer;" @click="refreshCaptcha">
          </el-form-item>

          
          <div style="display: flex; align-items: center;">
            <span style="font-size: 13px">已经注册了？请<a style="font-size: 13px;color: #668fd5" href="/login">登录</a></span>
            <el-button style="margin-left: 150px;" size="large" type="primary" @click="checkCaptcha">注册</el-button>
          </div>

        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import {User, Lock} from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const validatePass = (rule, value, callback) => {
  console.log(value)
  if (!value) {
    callback(new Error('请输入确认密码'))
  } else if (value !== data.form.password) {
    callback(new Error('两次密码不一致'))
  }else {
    callback()
  }
}

const data = reactive({

  form: {
    userName: null,
    password: null,
    confirmPassword:null,
    captcha: null

  },
  rules:{
    userName:[{required:true, message:'请输入账号',trigger:'blur'}],
    password:[{required:true, message:'请输入密码',trigger:'blur'}],
    confirmPassword:[{validator : validatePass,trigger:'blur'}]
  },
  captchaUrl: '',

});

const formRef = ref();

const register = () => {
  localStorage.setItem('jwt', '1');
  formRef.value.validate((valid) => {
    if (valid) {
          request.post('/register', data.form).then(res => {
            if (res.code === 200 || res.msg === '请求成功') {
              ElMessage.success('注册成功');
              setTimeout(() => {
                location.href = "/manager/home";
              }, 500);
              localStorage.setItem('UN', JSON.stringify(res.data));
            } else {
              ElMessage.error(res.msg);
              refreshCaptcha()
            }
          }).catch(error => {
            console.error(error);
            ElMessage.error('注册失败，请重试');
            refreshCaptcha()
          });
        } else {
      ElMessage.warning('请完整填写账号密码');
      refreshCaptcha()
    }
  });
};


const refreshCaptcha = () => {
  // 生成新的验证码图片URL
  data.captchaUrl = `${request.defaults.baseURL}/generate?time=${Date.now()}`;

};

refreshCaptcha()

const checkCaptcha = async () => {
  console.log("User input captcha: ", { input: data.form.captcha });
  try {
    const response = await request.post('/validate', data.form.captcha);
    console.log(response)
    if (response.code===200||response.msg==="请求成功") {
      register()
      return true;
    } else {
      ElMessage.error('验证码错误')
      refreshCaptcha()
      return false;
    }
  } catch (error) {
    refreshCaptcha()
    console.error(error);
    ElMessage.error('验证码验证失败，请重试');
    return false;
  }
}

</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  background-image: url("@/assets/img_2.png");
  background-size: cover;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-main {
  width: auto;
  padding: 20px;
  display: flex;
  justify-content: center; /* 水平居中内容 */
  align-items: center;    /* 垂直居中内容 */
}
</style>
