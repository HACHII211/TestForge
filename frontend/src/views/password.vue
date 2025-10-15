<template>
  <div>
    <el-card style="width: 60%;margin:10px;position: relative">
      <el-form :rules="data.rules" ref="formRef" :model="data.form" style="margin:50px">
        <el-form-item label="原密码" label-width="160px" style="width: 80%" prop="oldPassword">
          <el-input v-model="data.form.oldPassword" autocomplete="off" />
        </el-form-item>

        <el-form-item label="新密码" label-width="160px" style="width: 80%" prop="newPassword">
          <el-input v-model="data.form.newPassword" autocomplete="off" show-password/>
        </el-form-item>

        <el-form-item label="请再次输入新密码" label-width="160px" style="width: 80% " prop="newPassword2">
          <el-input v-model="data.form.newPassword2" autocomplete="off" show-password/>
        </el-form-item>

        <el-button type="primary" @click="updatePassword" style="position: absolute;bottom: 20px; right: 20px;" size="large">立即修改</el-button>
      </el-form>
    </el-card>

  </div>
</template>

<script setup>

import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const validateNewPass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入确认密码'))
  } else if (value !== data.form.newPassword) {
    callback(new Error('两次密码不一致'))
  }else if (value === data.form.oldPassword){
    callback(new Error('新密码不可和原密码相同'))
  } else {
    callback()
  }
}

const validateOldPass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入原密码'))
  } else if (value !== data.usr.password) {
    callback(new Error('原密码错误'))
  }else {
    callback()
  }
}



const data = reactive({
  form:{
    JWT: JSON.parse(localStorage.getItem('JWT')),
  },
  usr: JSON.parse(localStorage.getItem('UN')),
  rules:{

    oldPassword:[{validator: validateOldPass,trigger:'blur'}],
    newPassword:[{required:true,message:'请输入新密码',trigger:'blur'}],
    newPassword2:[{validator: validateNewPass,trigger:'blur'}]
  },
  newInfo:{}

})


const formRef = ref()

const getNewInfo_admin = () =>{
  request.get(`/admin/selectById/${data.form.id}`).then(res =>{
    data.newInfo = res.data
    console.log(res.data)
    console.log(data.newInfo)

  })
}

const getNewInfo_emp = () =>{
  request.get(`/employee/selectById/${data.form.empNo}`).then(res =>{
    data.newInfo = res.data
    console.log(res.data)
  })
}

const updatePassword = () => {

  formRef.value.validate((valid) => {
    if (valid) {
      if (data.usr.role === 'EMP') {
        data.form.empNo = data.usr.empNo
        request.put('/updatePassword',data.form).then(res => {
          if (res.code === '200') {
            ElMessage.success('修改成功')
            localStorage.removeItem('UN')
            getNewInfo_emp()
            setTimeout(() => {
              localStorage.setItem('UN',JSON.stringify(data.newInfo))
              location.href="/manager/home"
            }, 500)
            localStorage.setItem('UN',JSON.stringify(data.newInfo))
            setTimeout(() => {
              location.href('/manager')
            }, 500)
          } else {
            ElMessage.error(res.msg)
          }
        })
      } else {
        data.form.id = data.usr.id
        request.put('/updatePassword',data.form).then(res => {
          if (res.code === '200') {
            ElMessage.success('修改成功')
            localStorage.removeItem('UN')
            getNewInfo_admin()
            setTimeout(() => {
              localStorage.setItem('UN',JSON.stringify(data.newInfo))
              location.href="/manager/home"
            }, 500)
          } else {

            ElMessage.error(res.msg)
          }
        })
      }
    } else {
      ElMessage.error('请检查两次密码是否一致或原密码是否正确')
    }
  })
}




</script>

<style>

</style>