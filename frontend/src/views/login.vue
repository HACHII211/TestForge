<template>
  <div class="login-container">
    <div class="login-main">
      <img src="@/assets/logo.png" alt="" style="width: 250px;margin-right:160px;  height: auto;" />

      <el-card style="background-color: #e8faf2; padding: 20px">
        <span style="font-size: 21px;font-weight:bold;color: #3d52d7;display: flex">欢迎来到职场云☁！
          <el-select style="width: 37%;left: 46px;font-weight:normal" placeholder="选择登陆身份" v-model="data.form.role">
          <el-option label="员工" value="EMP"/>
          <el-option label="管理员" value="ADM"/>
        </el-select>
        </span>

        <el-form ref="formRef" :model="data.form" style="margin-top: 20px;" :rules="data.rules">
          <el-form-item label="账号:" prop="userName">
            <el-input placeholder="请输入账号" v-model="data.form.userName" style="width: 290px" :prefix-icon="User"></el-input>
          </el-form-item>
          <el-form-item label="密码:" prop="password">
            <el-input placeholder="请输入密码" v-model="data.form.password" style="width: 290px" show-password :prefix-icon="Lock"></el-input>
          </el-form-item>
          <div style="display: flex; align-items: center;">
            <span style="font-size: 13px">还没有注册嘛？请先<a style="font-size: 13px;color: #668fd5" href="/register">注册</a></span>
            <el-button style="margin-left: 110px;" size="large" type="primary" @click="login">登录</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import {User,Lock} from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const data = reactive({
  form: {
    role:'ADM'
  },
  rules:{
    userName:[{required:true, message:'请输入账号',trigger:'blur'}],
    password:[{required:true, message:'请输入密码',trigger:'blur'}]
  }

});

const formRef = ref();

const login = () => {
  formRef.value.validate((valid) => {
    if (valid){
      localStorage.setItem('jwt', '1');
      request.post('/login',data.form).then(res=>{
        if (res.code === 200 || res.msg === '请求成功'){
          localStorage.setItem('UN',JSON.stringify(res.data))
          ElMessage.success('登陆成功')
          setTimeout(() =>{
            location.href="/manager/home"
          },500)
        } else {
          ElMessage.error(res.msg)
        }
      })
    } else {ElMessage.warning('请完整填写账号密码')
    }
  })
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
