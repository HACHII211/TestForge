<template>
  <div class="page-root">
    <div class="top-bar">
      <div class="title">测试计划</div>
      <div class="actions">
        <el-button type="primary" @click="openCreate">新建计划</el-button>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="q" placeholder="按计划名称搜索" clearable @keydown.enter.native="search" style="min-width:240px; width:30vw" />
        <el-select v-model="filterProjectId" placeholder="按项目筛选" clearable style="min-width:160px; width:18vw" @change="search">
          <el-option :label="'全部项目'" :value="null" />
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>

        <div class="toolbar-right">
          <el-button @click="reset">重置</el-button>
        </div>
      </div>

      <el-table :data="plans" stripe style="width:100%; margin-top:12px" :row-key="row => row.id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="计划名称" min-width="300" />
        <el-table-column prop="projectId" label="项目" width="180">
          <template #default="scope">
            {{ findProjectName(scope.row.projectId) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="ownerId" label="负责人" width="140">
          <template #default="scope">
            {{ findUserName(scope.row.ownerId) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status" :type="scope.row.status === 'Active' ? 'success' : (scope.row.status === 'Draft' ? 'info' : '')">
              {{ scope.row.status }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="周期" width="220">
          <template #default="scope">
            <div>{{ formatDate(scope.row.startDate) }} — {{ formatDate(scope.row.endDate) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="mini" @click="goToDetail(scope.row)">执行</el-button>
            <el-button type="warning" size="mini" @click="openEdit(scope.row)" style="margin-left:8px">编辑</el-button>
            <el-button type="danger" size="mini" @click="confirmDelete(scope.row)" style="margin-left:8px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="bottom-bar">
        <div class="left-info">共 {{ total }} 条</div>
        <div class="pager">
          <el-pagination
              background
              :current-page="pageNum"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog :title="editing ? '编辑测试计划' : '新建测试计划'" v-model="dialogVisible" width="720px" :destroy-on-close="true">
      <el-form :model="form" label-width="100px" ref="planForm">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>

        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="选择项目" clearable>
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="负责人" prop="ownerId">
          <el-select v-model="form.ownerId" placeholder="选择负责人" clearable>
            <el-option v-for="u in users" :key="u.id" :label="u.username || u.name" :value="u.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始 / 结束">
          <el-date-picker v-model="form.startDate" type="date" placeholder="开始日期" style="width:45%" />
          <el-date-picker v-model="form.endDate" type="date" placeholder="结束日期" style="width:45%; margin-left:10px" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="状态">
            <el-option label="Draft" value="Draft" />
            <el-option label="Active" value="Active" />
            <el-option label="Completed" value="Completed" />
            <el-option label="Cancelled" value="Cancelled" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" rows="4" />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan">保存</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();

const plans = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const q = ref('');
const filterProjectId = ref(null);

const projects = ref([]);
const users = ref([]); // 简单的用户列表

// dialog / form
const dialogVisible = ref(false);
const editing = ref(false);
const form = reactive({
  id: null,
  name: '',
  projectId: null,
  moduleId: null,
  description: '',
  ownerId: null,
  status: 'Draft',
  startDate: null,
  endDate: null
});
const planForm = ref(null);

const loadProjects = async () => {
  try {
    const resp = await request.get('/projects', { params: { pageNum: 1, pageSize: 200 } });
    const body = resp.data || {};
    const list = body.list || (body.data && body.data.list) || [];
    projects.value = list.map(p => ({ id: p.id, name: p.name }));
  } catch (err) {
    console.error(err);
  }
};

const loadUsers = async () => {
  try {
    const resp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 200 } });
    const body = resp.data || {};
    const list = body.list || (body.data && body.data.list) || [];
    users.value = list.map(u => ({ id: u.id, username: u.username || u.name }));
  } catch (err) {
    console.warn('加载用户失败（可忽略）', err);
    users.value = [];
  }
};

const load = async () => {
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (q.value) params.name = q.value;
    if (filterProjectId.value) params.projectId = filterProjectId.value;
    const resp = await request.get('/test-plans', { params });
    const body = resp.data || {};
    const page = body.list ? body : (body.data ? body.data : body);
    plans.value = page.list || (body.data && body.data.list) || [];
    total.value = page.total || (body.data && body.data.total) || 0;
  } catch (err) {
    console.error(err);
    ElMessage.error('加载计划失败');
  }
};

const search = () => {
  pageNum.value = 1;
  load();
};

const reset = () => {
  q.value = '';
  filterProjectId.value = null;
  pageNum.value = 1;
  load();
};

const handlePageChange = (p) => {
  pageNum.value = p;
  load();
};

const formatDate = (v) => {
  if (!v) return '-';
  const d = new Date(v);
  if (isNaN(d.getTime())) return v;
  const Y = d.getFullYear();
  const M = String(d.getMonth() + 1).padStart(2, '0');
  const D = String(d.getDate()).padStart(2, '0');
  return `${Y}-${M}-${D}`;
};

const findProjectName = (id) => {
  const p = projects.value.find(x => x.id === id);
  return p ? p.name : null;
};
const findUserName = (id) => {
  const u = users.value.find(x => x.id === id);
  return u ? u.username : null;
};

// 创建 / 编辑
const openCreate = () => {
  editing.value = false;
  Object.assign(form, { id: null, name: '', projectId: null, moduleId: null, description: '', ownerId: null, status: 'Draft', startDate: null, endDate: null });
  dialogVisible.value = true;
};
const openEdit = (row) => {
  editing.value = true;
  Object.assign(form, {
    id: row.id,
    name: row.name,
    projectId: row.projectId,
    moduleId: row.moduleId,
    description: row.description,
    ownerId: row.ownerId,
    status: row.status,
    startDate: row.startDate,
    endDate: row.endDate
  });
  dialogVisible.value = true;
};
const savePlan = async () => {
  try {
    if (!form.name || !form.name.trim()) {
      ElMessage.warning('请填写计划名称');
      return;
    }
    if (editing.value) {
      await request.put(`/test-plans/${form.id}`, form);
      ElMessage.success('更新成功');
    } else {
      await request.post('/test-plans', form);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    load();
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

// 删除
const confirmDelete = (row) => {
  ElMessageBox.confirm(`确认删除测试计划：${row.name} ?`, '删除确认', { type: 'warning' })
      .then(async () => {
        try {
          await request.delete(`/test-plans/${row.id}`);
          ElMessage.success('删除成功');
          load();
        } catch (err) {
          console.error(err);
          ElMessage.error('删除失败');
        }
      })
      .catch(() => {});
};

// 跳转到执行页（详情）
const goToDetail = (row) => {
  // 路由到 /test-plan/testPlanDetail?id=xxx
  // 若你使用 vue-router，推荐使用 router.push
  router.push({ path: '/test-plan/testPlanDetail', query: { id: row.id } });
};

onMounted(async () => {
  await loadProjects();
  await loadUsers();
  await load();
});
</script>

<style scoped>
.page-root { padding: 16px; background: #f7fafc; min-height: 100vh; box-sizing: border-box; }
.top-bar { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:12px; }
.title { font-size:20px; font-weight:700; color:#111827; }
.card { background:#fff; border-radius:12px; padding:16px; box-shadow:0 8px 20px rgba(15,23,42,0.06); }
.toolbar { display:flex; align-items:center; gap:10px; }
.toolbar-right { margin-left:auto; }
.bottom-bar { display:flex; justify-content:space-between; align-items:center; margin-top:12px; }
.left-info { color:#6b7280; font-size:13px; }
.pager { }
.dialog-footer { display:flex; justify-content:flex-end; gap:10px; }
</style>
