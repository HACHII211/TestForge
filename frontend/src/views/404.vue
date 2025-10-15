<template>
  <div style="text-align:center">
    <img src="../assets/img_1.png" alt="肥肠抱歉"/>
    <div style="text-align: center"><el-button type="primary" style="width: 150px;height: 60px" @click="router.back()">返回上一级~</el-button></div>

  </div>
</template>

<script setup>
import router from "@/router/index.js";
</script>