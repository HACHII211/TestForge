<template>
  <div>
    <el-card class="schedule-card" shadow="never" style="margin: 20px;">
      <div class="schedule-header">
        <h2>我的日程管理</h2>
        <div>
          <el-button type="success" @click="aiDialogVisible = true">AI 一键编排日程</el-button>
          <el-button type="primary" @click="openCreateDialog">新增日程</el-button>
        </div>
      </div>

      <!-- AI 对话框 -->
      <el-dialog v-model="aiDialogVisible" title="AI 编排日程" width="600px">
        <el-form label-width="80px">
          <el-form-item label="主题">
            <el-input v-model="data.extraTitle" placeholder="请输入日程主题" />
          </el-form-item>

          <el-input
              v-model="data.extraContent"
              type="textarea"
              :rows="12"
              placeholder="这里显示AI生成的内容"
              style="width: 100%;"
          />

        </el-form>
        <template #footer>
          <el-button @click="aiDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="generateAIContent">生成</el-button>
        </template>
      </el-dialog>

      <el-table :data="todaySchedules" style="margin-top: 20px;">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="time" label="时间">
          <template #default="{ row }">
            {{ row.startTime }} - {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="mini" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteSchedule(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新增／编辑对话框 -->
      <el-dialog :title="dialogTitle" v-model="dialogVisible">
        <el-form :model="form" ref="formRef" label-width="80px">
          <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入标题', trigger: 'blur' }]">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="日期" prop="date" :rules="[{ required: true, message: '请选择日期', trigger: 'change' }]">
            <el-date-picker
                v-model="form.date"
                type="date"
                placeholder="选择日期"
                style="width: 100%;"
                value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime" :rules="[{ required: true, message: '请选择开始时间', trigger: 'change' }]">
            <el-time-picker
                v-model="form.startTime"
                placeholder="HH:mm"
                format="HH:mm"
                value-format="HH:mm"
                style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime" :rules="[{ required: true, message: '请选择结束时间', trigger: 'change' }]">
            <el-time-picker
                v-model="form.endTime"
                placeholder="HH:mm"
                format="HH:mm"
                value-format="HH:mm"
                style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input type="textarea" v-model="form.content" rows="3" />
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="onSubmit">提交</el-button>
        </span>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElDialog, ElForm } from 'element-plus'
import request from '@/utils/request'

// 表格数据
const todaySchedules = ref([])

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('新增日程')
const formRef = ref(null)

// AI 对话框控制
const aiDialogVisible = ref(false)

const data = reactive({
  extraTitle: '',
  extraContent: ''
})

const generateAIContent = async () => {
  if (!data.extraTitle) {
    ElMessage.warning('请输入标题！');
    return;
  }

  data.extraContent = ''; // 清空旧内容

  try {
    const response = await fetch('http://localhost:9090/qa/normalAsk', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: localStorage.getItem('jwt') || ''
      },
      body: JSON.stringify({
        question: data.extraTitle,
        require: '请你根据用户提供的主题生成完整规范的部门通知内容'
      })
    });

    if (!response.ok || !response.body) {
      throw new Error('无法获取流响应');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (let line of lines) {
        line = line.trim();
        if (!line || line === 'data: [DONE]') continue;

        line = line.replace(/^data:\s*/, '');
        try {
          const json = JSON.parse(line);
          const delta = json.choices?.[0]?.delta?.content;
          if (delta) {
            data.extraContent += delta; // 追加
          }
        } catch (e) {
          // 忽略 JSON 解析错误
        }
      }
    }
  } catch (error) {
    console.error('生成内容失败', error);
    ElMessage.error('生成内容失败');
  }
};

// 表单模型
const form = reactive({
  id: null,
  title: '',
  date: '',
  startTime: '',
  endTime: '',
  content: ''
})

// 拉取今天的所有日程
async function fetchSchedules() {
  try {
    const res = await request.get('/schedule/selectAll')
    // 假设后端返回 data: [ { id, title, date, startTime, endTime, content, … } ]
    todaySchedules.value = res.data
  } catch (err) {
    ElMessage.error('获取日程失败')
    console.error(err)
  }
}

// 打开“新增”对话框
function openCreateDialog() {
  dialogVisible.value = true
  dialogTitle.value = '新增日程'
  Object.assign(form, { id: null, title: '', date: '', startTime: '', endTime: '', content: '' })

}

// 打开“编辑”对话框（深拷贝行数据到 form）
function openEditDialog(row) {

  dialogTitle.value = '编辑日程'
  // 深拷贝
  Object.assign(form, JSON.parse(JSON.stringify({
    id: row.id,
    title: row.title,
    date: row.date,
    startTime: row.startTime,
    endTime: row.endTime,
    content: row.content
  })))
  dialogVisible.value = true
}

// 提交表单（新增或编辑）
async function onSubmit() {
  await formRef.value.validate(async valid => {
    if (!valid) return
    try {
      if (form.id) {
        // 编辑
        await request.put('/schedule/update', form)
        ElMessage.success('编辑成功')
      } else {
        // 新增
        await request.post('/schedule/add', form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      fetchSchedules()
    } catch (err) {
      ElMessage.error('保存失败')
      console.error(err)
    }
  })
}

// 删除
async function deleteSchedule(id) {
  try {
    await request.delete(`/schedule/delete/${id}`)
    ElMessage.success('删除成功')
    fetchSchedules()
  } catch (err) {
    ElMessage.error('删除失败')
    console.error(err)
  }
}

// 组件挂载后拉取数据
fetchSchedules()
</script>

<style scoped>
.schedule-card { padding: 20px; }
.schedule-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.dialog-footer { text-align: right; }
</style>
