<template>
  <div>
    <el-card style="margin: 10px;width: 50%">
    <el-form :rules="data.rules" ref="formRef" :model="data.form" style="margin:50px">
      <el-form-item label-width="80px" label="头像">
      <div style="width: 40%;justify-content: center;display: flex">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:9090/files/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
          >
            <img v-if="data.form.avatar" :src="data.form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
      </div>
      </el-form-item>

      <el-form-item label="账号" label-width="80px" prop="userName">
        <el-input v-model="data.form.userName" autocomplete="off" disabled/>
      </el-form-item>

      <div v-if="data.user.role === 'ADM'">
        <el-form-item label="名字" label-width="80px" prop="name">
          <el-input v-model="data.form.name" autocomplete="off" placeholder="请输入名字"/>
        </el-form-item>
      </div>

      <div v-if="data.user.role==='EMP'">
        <el-form-item label="工号" label-width="80px" prop="empNo">
          <el-input v-model="data.form.empNo" autocomplete="off" disabled/>
        </el-form-item>

        <el-form-item label="名字" label-width="80px" prop="firstName">
          <el-input v-model="data.form.firstName" autocomplete="off" placeholder="请输入名字"/>
        </el-form-item>

        <el-form-item label="姓氏" label-width="80px" prop="lastName">
          <el-input v-model="data.form.lastName" autocomplete="off" placeholder="请输入姓氏"/>
        </el-form-item>

        <el-form-item label="出生日期" label-width="80px" prop="birthDate">
          <el-date-picker v-model="data.form.birthDate"
                          default-value="2004-01-01"
                          type="date"/>
        </el-form-item>

        <el-form-item label="性别" label-width="80px">
          <el-radio-group v-model="data.form.gender">
            <el-radio label="男" value="M"/>
            <el-radio label="女" value="F"/>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="入职时间" label-width="80px">
          <el-date-picker v-model="data.form.hireDate" />
        </el-form-item>


      </div>

      <div style="display: flex;justify-content: flex-end;margin-top: 60px">
        <el-button size="large" type="danger" @click="cancel">取消修改</el-button>
        <el-button size="large" type="success" @click="updater">确认修改</el-button>
      </div>

    </el-form>
    </el-card>
  </div>
</template>


<script setup>

import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import {Lock, Plus} from "@element-plus/icons-vue";

const data = reactive({

  user: JSON.parse(localStorage.getItem('UN')),

  rules: {
    firstName:[{required : true,message: '记得填写姓名哦~',trigger:"blur"}],
    name:[{required : true,message: '记得填写姓名哦~',trigger:"blur"}],
    lastName:[{required : true,message: '记得填写姓氏哦~',trigger:"blur"}],
    birthDate:[{required : true,message: '记得填写生日哦~',trigger:"blur"}]
  },

  form:{
  },

})

const handleAvatarSuccess = (res) =>{
  console.log(res.data)
  data.form.avatar = res.data
}

const formRef = ref()

if (data.user.role === 'EMP'){
  request.get(`/employee/selectUser/${data.user.userName}`).then(res=>{
    data.form=res.data
  })
}else {
  data.form = data.user
}

const emit = defineEmits(['updater'])

const cancel = () => {
  setTimeout(() =>{
    location.href="/manager/home"
  },500)
}

const updater = () => {
  formRef.value.validate((valid) =>{
    if (valid){
      if(data.user.role === 'EMP'){
        request.put(`/employee/update`,data.form).then(res=>{
          if (res.code === '200'){
            ElMessage.success('更新成功')
            localStorage.setItem('UN',JSON.stringify(data.form))
            emit('updater')
            cancel()
          } else {
            ElMessage.error(res.msg)
          }
        })
      } else {
        request.put(`/admin/update`,data.form).then(res=>{
          if (res.code === '200'){
            request.put(`/admin/update`,data.form).then(res=>{
              if (res.code === '200'){
                ElMessage.success('更新成功')
                localStorage.setItem('UN',JSON.stringify(data.form))
                emit('updater')
                cancel()
              } else {
                ElMessage.error(res.msg)
              }
            })
          }
        })
      }
    } else {
      ElMessage.error('请填写完整信息')
    }

  })
}

</script>

<style scoped>
.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  display: block;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
}
</style>