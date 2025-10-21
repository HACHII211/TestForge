<template>
  <div style="display: flex; flex-direction: column; height: 100%;">
    <!-- 顶部栏 -->
    <div style="height: 60px; background-color: white; text-align: left; color: #668fd5; font-size: 35px; display: flex; align-items: center">
      <div style="width: 200px; display: flex; align-items: center; font-size: 25px">
        <img style="width: 50px" src="@/assets/TestForge.png" alt="">
        Test Forge
      </div>
      <div style="margin-left: 70vw" />
<!--      <div style="align-items: center; display: flex; width: fit-content">-->
<!--        <img :src="data.UN.avatar" alt="" style="border-radius: 50%; width: 55px; height: 50px">-->
<!--        <span style="font-size: 20px" v-if="data.UN.role === 'ADM'">{{ data.UN.name }}</span>-->
<!--        <span style="font-size: 20px" v-if="data.UN.role === 'EMP'">{{ data.UN.firstName }}</span>-->
<!--      </div>-->
    </div>
    <!-- 头部区域开始 -->
    <div style="border: 1px solid #ddd; display: flex; flex: 1;">
      <!-- 左侧导航菜单 -->
      <div style="width: 200px; border-right: 1px solid #ddd; min-height: calc(100vh - 60px);">
        <el-menu router :default-active="router.currentRoute.value.path" style="border: 1px" class="el-menu">
<!--          <el-menu-item index="/testforge/home">-->
<!--            <el-icon><HomeFilled /></el-icon>-->
<!--            首页-->
<!--          </el-menu-item>-->
<!--          <el-menu-item index="/testforge/employee" v-if="data.UN.role === 'ADM'">-->
<!--            <el-icon><Money /></el-icon>-->
<!--            员工信息-->
<!--          </el-menu-item>-->
<!--          <el-menu-item index="/testforge/admin" v-if="data.UN.role === 'ADM'">-->
<!--            <el-icon><SuitcaseLine /></el-icon>-->
<!--            管理员信息-->
<!--          </el-menu-item>-->
          <el-menu-item index="/testforge/project">
            <el-icon><Reading /></el-icon>
            项目
          </el-menu-item>
          <el-menu-item index="/testforge/organization">
            <el-icon><Compass /></el-icon>
            组织
          </el-menu-item>
          <el-menu-item index="/testforge/defect">
            <el-icon><SuitcaseLine /></el-icon>
            缺陷库
          </el-menu-item>
          <el-menu-item index="/testforge/testCase">
            <el-icon><Calendar /></el-icon>
            用例库
          </el-menu-item>
          <el-menu-item index="/testforge/qa">
            <el-icon><el-icon-chat-dot-square /></el-icon>
            智能助手
          </el-menu-item>
<!--          <el-sub-menu>-->
<!--            <template #title>-->
<!--              <span><el-icon><User /></el-icon></span>-->
<!--              <span>用户中心</span>-->
<!--            </template>-->
<!--            <el-menu-item index="/testforge/personInfo">用户信息</el-menu-item>-->
<!--            <el-menu-item index="/testforge/password">修改密码</el-menu-item>-->
<!--          </el-sub-menu>-->
          <el-menu-item @click="logOut">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧主体区域 -->
      <div class="right-content">
        <RouterView @updater="handleUpdater" />
      </div>
    </div>
  </div>
</template>

<script setup>
import router from "@/router/index.js";
import { reactive } from "vue";
import { ElMessageBox } from "element-plus";
import {
  Calendar,
  Compass, HomeFilled, Money, Odometer, Reading, SuitcaseLine, SwitchButton, User
} from "@element-plus/icons-vue";

const data = reactive({
  UN: JSON.parse(localStorage.getItem('UN')),
});

const logOut = () => {
  // 弹出确认框
  ElMessageBox.confirm(
      '确定要退出登录吗？',  // 确认框内容
      '退出登录',             // 标题
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        localStorage.removeItem('auth.expiresAt');
        localStorage.removeItem('auth.token');
        localStorage.removeItem('app_permissions');

        // 跳转到登录页面
        location.href = '/login';
        console.log('用户已退出登录');
      })
      .catch(() => {
        // 用户点击“取消”后的操作
        console.log('取消退出');
      });
};

const handleUpdater = () => {
  data.UN = JSON.parse(localStorage.getItem('UN'))
};
</script>
<style>
body, html {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;  /* 防止外部滚动条 */
}

.el-menu .is-active {
  background-color: rgba(221, 204, 229, 0.19) !important;
}

.el-sub-menu__title {
  background-color: white;
}

/* 主体区域 */
.main-content {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); /* 减去顶部导航栏高度 */
  overflow: hidden; /* 防止滚动条 */
}

/* 右侧区域内容 */
.right-content {
  flex: 1;
  overflow: auto; /* 如果内容超出，可以滚动 */
  background-color: #f5f7ff;
}
</style>
