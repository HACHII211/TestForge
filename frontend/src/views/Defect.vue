<template>
  <div class="page-root">
    <div class="page-container">
      <!-- 顶部跨栏卡片 -->
      <div class="card top-card" ref="topCard">
        <div class="top-left">
          <label class="small-label">项目 </label>
          <el-select
              v-model="state.selectedProjectId"
              placeholder="全部项目"
              clearable
              filterable
              @change="onProjectChange"
              :style="{ minWidth: '220px', width: 'min(40vw, 320px)' }"
          >
            <el-option :label="'全部项目'" :value="null" />
            <el-option
                v-for="p in state.projects"
                :key="p.id ?? p._val"
                :label="p.name"
                :value="p.id ?? null"
            />
          </el-select>
        </div>

        <div class="top-right">
          <el-input
              v-model="state.q"
              placeholder="按缺陷标题搜索（回车查询）"
              @keydown.enter.native="onSearch"
              clearable
              :style="{ width: 'min(40vw, 420px)' }"
          />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <!-- 左右列 -->
      <div class="main-row">
        <!-- 左侧模块 -->
        <div class="card left-card">
          <div class="left-card-header">模块</div>

          <div class="module-list" ref="moduleList">
            <div
                class="module-chip"
                :class="{ active: state.selectedModuleId === null }"
                @click="selectModule(null)"
                title="全部模块"
            >
              全部模块
            </div>

            <div
                v-for="m in state.modules"
                :key="m.id ?? m._val"
                class="module-chip"
                :class="{ active: isModuleSelected(m) }"
                @click="selectModule(m)"
                :title="getModuleDisplayName(m)"
            >
              {{ getModuleDisplayName(m) }}
            </div>
          </div>
        </div>

        <!-- 右侧表格 -->
        <div class="card right-card">
          <div class="right-toolbar">
            <div class="left-actions">
              <el-button type="danger" @click="deleteSelected" :disabled="!selectedRows.length" v-if="hasPermission('defect_manage')">批量删除</el-button>
              <el-button type="primary" @click="openAddDrawer" style="margin-left: 8px" v-if="hasPermission('defect_manage')">新增缺陷</el-button>
            </div>

            <div class="right-actions">
              <div class="total-text">共 {{ state.total }} 条</div>
            </div>
          </div>

          <div class="table-viewport">
            <el-table
                :data="tableData"
                @selection-change="onSelectionChange"
                stripe
                size="medium"
                :row-key="row => row.id"
                style="width: 100%; min-width: 800px"
            >
              <el-table-column type="selection" width="55" v-if="hasPermission('defect_manage')"/>
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="title" label="标题" min-width="260" />
              <el-table-column label="优先级" width="120">
                <template #default="scope">
                  <el-tag :type="priorityTagType(resolvePriorityName(scope.row))">
                    {{ resolvePriorityName(scope.row) || '-' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="140">
                <template #default="scope">
                  <el-tag :style="statusTagStyle(resolveStatusName(scope.row))">
                    {{ resolveStatusName(scope.row) || '-' }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="项目" prop="projectName" width="160">
                <template #default="scope">
                  {{ getProjectName(scope.row) }}
                </template>
              </el-table-column>

              <el-table-column label="模块" prop="moduleName" width="160">
                <template #default="scope">
                  {{ getModuleName(scope.row) }}
                </template>
              </el-table-column>

              <el-table-column label="提交人" prop="createdByName" width="140">
                <template #default="scope">
                  {{ scope.row.createdByName || '-' }}
                </template>
              </el-table-column>

              <el-table-column label="处理人" prop="assignedToName" width="140">
                <template #default="scope">
                  {{ scope.row.assignedToName || '-' }}
                </template>
              </el-table-column>

              <el-table-column label="创建时间" prop="createdAt" width="160">
                <template #default="scope">
                  {{ formatDate(scope.row.createdAt || scope.row.created_at) }}
                </template>
              </el-table-column>

              <el-table-column label="操作" width="160" fixed="right" v-if="hasPermission('defect_manage')">
                <template #default="scope">
                  <span class="action-link" @click="openEditDrawer(scope.row)">编辑</span>
                  <span class="action-link danger" @click="confirmDelete(scope.row)">删除</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="bottom-pagination">
            <el-pagination
                :current-page="state.pageNum"
                :page-size="state.pageSize"
                :total="state.total"
                layout="total, prev, next"
                @current-change="handlePageChange"
                background
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <el-dialog title="确认删除" v-model="confirmDialog.visible" width="460">
      <div style="text-align:center; font-size:16px; margin: 24px 0;">
        确认删除缺陷：<strong>{{ confirmDialog.row?.id }} - {{ confirmDialog.row?.title }}</strong> ?
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="confirmDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="doDelete">确认删除</el-button>
      </span>
    </el-dialog>

    <!-- 新增/编辑抽屉 -->
    <el-drawer
        v-model="drawer.visible"
        :with-header="false"
        direction="rtl"
        :size="'46vw'"
        append-to-body
        class="drawer-custom"
        :destroy-on-close="true"
    >
      <div class="drawer-header">
        <div class="drawer-title">{{ drawer.isEdit ? '编辑缺陷' : '新增缺陷' }}</div>
        <div class="drawer-actions">
          <el-button type="text" @click="drawer.visible = false">关闭</el-button>
        </div>
      </div>

      <div class="drawer-body">
        <el-form :model="drawer.form" ref="drawerFormRef" label-width="110px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="drawer.form.title" />
          </el-form-item>

          <el-form-item label="所属项目" prop="projectId">
            <el-select v-model="drawer.form.projectId" placeholder="选择项目" @change="onDrawerProjectChange" clearable>
              <el-option v-for="p in state.projects" :key="p.id ?? p._val" :label="p.name" :value="p.id ?? null" />
            </el-select>
          </el-form-item>

          <el-form-item label="所属模块" prop="moduleId">
            <el-select v-model="drawer.form.moduleId" placeholder="选择模块" clearable>
              <el-option
                  v-for="m in drawer.modulesFiltered"
                  :key="m.id ?? m._val"
                  :label="getModuleDisplayName(m)"
                  :value="m.id ?? null"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="优先级" prop="priorityId">
            <el-select v-model="drawer.form.priorityId" placeholder="选择优先级" clearable>
              <el-option v-for="p in state.priorities" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态" prop="statusId">
            <el-select v-model="drawer.form.statusId" placeholder="选择状态" clearable>
              <el-option v-for="s in state.statuses" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="处理人" prop="assignedTo">
            <el-select v-model="drawer.form.assignedTo" clearable>
              <el-option v-for="u in state.users" :key="u.id ?? u._val" :label="u.username" :value="u.id ?? null" />
            </el-select>
          </el-form-item>

          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="drawer.form.description" rows="6" />
          </el-form-item>
        </el-form>
      </div>

      <div class="drawer-footer">
        <el-button @click="drawer.visible=false">取消</el-button>
        <el-button type="primary" @click="saveDrawer">保存</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { hasPermission } from '@/utils/perm';

const state = reactive({
  projects: [],
  modules: [],
  users: [],
  priorities: [],
  statuses: [],
  selectedProjectId: null,
  selectedModuleId: null,
  q: '',
  pageNum: 1,
  pageSize: 10,
  total: 0,
  defects: [],
});

const selectedRows = ref([]);
const drawerFormRef = ref(null);

const drawer = reactive({
  visible: false,
  isEdit: false,
  form: {
    id: null,
    title: '',
    description: '',
    projectId: null,
    moduleId: null,
    priorityId: null,
    statusId: null,
    assignedTo: null,
  },
  modulesFiltered: [],
});

const confirmDialog = reactive({
  visible: false,
  row: null,
});

// helpers
const extractList = (resp) => {
  const body = resp?.data || {};
  return body.list || (body.data && body.data.list) || body || [];
};
const extractPageInfo = (resp) => {
  const body = resp?.data || {};
  return {
    list: body.list || (body.data && body.data.list) || [],
    total: body.total || (body.data && body.data.total) || (body.data && body.data.count) || 0,
  };
};

// loaders
const loadProjects = async () => {
  try {
    const resp = await request.get('/projects', { params: { pageNum: 1, pageSize: 200 } });
    const list = extractList(resp);
    state.projects = list.map(p => ({ id: p.id, name: p.name, _val: p.id }));
  } catch (err) {
    console.error(err);
    ElMessage.error('加载项目失败');
    state.projects = [];
  }
};

const loadModules = async (projectId = null) => {
  try {
    const params = { pageNum: 1, pageSize: 200 };
    if (projectId != null) params.projectId = projectId;
    const resp = await request.get('/modules', { params });
    const list = extractList(resp);
    // ensure projectId property exists on each module
    state.modules = list.map(m => ({
      id: m.id,
      name: m.name,
      projectId: m.projectId ?? m.project_id ?? m.projectId ?? null,
      _val: m.id
    }));
  } catch (err) {
    console.error(err);
    ElMessage.error('加载模块失败');
    state.modules = [];
  }
};

// load users from /organization/users
const loadUsers = async () => {
  try {
    const resp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 500 } });
    const list = extractList(resp);
    state.users = list.map(u => ({ id: u.id, username: u.username || u.name || u.userName, _val: u.id }));
  } catch (err) {
    console.warn('加载用户失败：', err);
    state.users = [];
  }
};

const loadPriorities = async () => {
  try {
    const resp = await request.get('/defect-priorities');
    const list = extractList(resp);
    state.priorities = list.map(x => ({ id: x.id, name: x.name, description: x.description }));
  } catch (err) {
    console.error(err);
    ElMessage.error('加载优先级失败');
    state.priorities = [];
  }
};

const loadStatuses = async () => {
  try {
    const resp = await request.get('/defect-statuses');
    const list = extractList(resp);
    state.statuses = list.map(x => ({ id: x.id, name: x.name, description: x.description }));
  } catch (err) {
    console.error(err);
    ElMessage.error('加载状态失败');
    state.statuses = [];
  }
};

// normalize row and map ids->names
const normalizeDefectRow = (r) => {
  const row = { ...r };
  row.projectId = row.projectId ?? row.project_id ?? null;
  row.moduleId = row.moduleId ?? row.module_id ?? null;
  row.priorityId = row.priorityId ?? row.priority_id ?? null;
  row.statusId = row.statusId ?? row.status_id ?? null;

  row.priorityName = row.priorityName ?? row.priority_name ?? row.priority ?? null;
  row.statusName = row.statusName ?? row.status_name ?? row.status ?? null;

  // createdBy / assignedTo mapping
  const createdId = row.createdBy ?? row.created_by ?? row.created_by_id ?? null;
  const assignedId = row.assignedTo ?? row.assigned_to ?? row.assigned_to_id ?? null;
  row.createdBy = createdId;
  row.assignedTo = assignedId;

  // name lookup from loaded users
  row.createdByName = row.createdByName ?? row.created_by_name ?? (createdId ? findUserNameById(createdId) : null) ?? null;
  row.assignedToName = row.assignedToName ?? row.assigned_to_name ?? (assignedId ? findUserNameById(assignedId) : null) ?? null;

  // project/module name fallbacks (backend may return these directly)
  row.projectName = row.projectName ?? row.project_name ?? (row.project && (row.project.name || row.project.projectName)) ?? null;
  row.moduleName = row.moduleName ?? row.module_name ?? (row.module && (row.module.name || row.module.moduleName)) ?? null;

  return row;
};

const loadDefects = async (pageNum = 1, pageSize = state.pageSize) => {
  try {
    const params = { pageNum, pageSize, title: state.q };
    if (state.selectedProjectId != null) params.projectId = state.selectedProjectId;
    if (state.selectedModuleId != null) params.moduleId = state.selectedModuleId;
    const resp = await request.get('/defect', { params });
    const pi = extractPageInfo(resp);
    // ensure users already loaded for name mapping
    state.defects = pi.list.map(normalizeDefectRow);
    state.total = pi.total;
    state.pageNum = pageNum;
    state.pageSize = pageSize;
  } catch (err) {
    console.error(err);
    ElMessage.error('加载缺陷失败');
    state.defects = [];
    state.total = 0;
  }
};

const tableData = computed(() => state.defects);

// name lookup helpers
const findUserNameById = (id) => {
  if (id == null) return null;
  const u = state.users.find(x => String(x.id) === String(id));
  return u ? u.username : null;
};
const findProjectName = (id) => {
  if (id == null) return null;
  const p = state.projects.find(x => String(x.id) === String(id));
  return p ? p.name : null;
};
const findModuleName = (id) => {
  if (id == null) return null;
  const m = state.modules.find(x => String(x.id) === String(id));
  return m ? m.name : null;
};

// Resolve priority/status names for display (prefer returned name, fallback to lookup by id)
const resolvePriorityName = (row) => {
  if (!row) return null;
  return row.priorityName || getPriorityNameById(row.priorityId) || null;
};
const resolveStatusName = (row) => {
  if (!row) return null;
  return row.statusName || getStatusNameById(row.statusId) || null;
};
const getPriorityNameById = (id) => {
  if (id == null) return null;
  const p = state.priorities.find(x => String(x.id) === String(id));
  return p ? p.name : null;
};
const getStatusNameById = (id) => {
  if (id == null) return null;
  const s = state.statuses.find(x => String(x.id) === String(id));
  return s ? s.name : null;
};

// Project/module display in table (prefer row names, else lookup)
const getProjectName = (row) => {
  if (!row) return '-';
  const nameFromRow = row.projectName ?? row.project_name ?? (row.project && (row.project.name || row.project.projectName));
  if (nameFromRow) return nameFromRow;
  return findProjectName(row.projectId) || '-';
};
const getModuleName = (row) => {
  if (!row) return '-';
  const nameFromRow = row.moduleName ?? row.module_name ?? (row.module && (row.module.name || row.module.moduleName));
  if (nameFromRow) return nameFromRow;
  return findModuleName(row.moduleId) || '-';
};

// Module display name for left list and drawer options
const getModuleDisplayName = (m) => {
  if (!m) return '';
  // if showing modules across ALL projects, append project name
  if (state.selectedProjectId == null) {
    const pjName = findProjectName(m.projectId);
    return pjName ? `${m.name} (${pjName})` : m.name;
  }
  // otherwise show plain module name
  return m.name;
};

// priority -> tag type (High:red, Medium:yellow, Low:green)
const priorityTagType = (pName) => {
  if (!pName) return 'info';
  const key = String(pName).toLowerCase();
  if (key.includes('high') || key.includes('h') || key === 'high') return 'danger';
  if (key.includes('medium') || key.includes('m') || key === 'medium') return 'warning';
  if (key.includes('low') || key.includes('l') || key === 'low') return 'success';
  // fallback: try exact matches in dictionary
  const found = state.priorities.find(x => String(x.name).toLowerCase() === key);
  if (found) {
    const n = String(found.name).toLowerCase();
    if (n.includes('high')) return 'danger';
    if (n.includes('medium')) return 'warning';
    if (n.includes('low')) return 'success';
  }
  return 'info';
};

// status styling unchanged (keeps previous palette)
const statusTagStyle = (s) => {
  if (!s) return { background: '#f3f4f6', color: '#374151' };
  const key = String(s).toLowerCase();
  if (key.includes('new') || key.includes('新')) return { background: '#e0f2fe', color: '#0369a1' };
  if (key.includes('progress') || key.includes('处理中') || key.includes('in progress')) return { background: '#fff7ed', color: '#b45309' };
  if (key.includes('resolved') || key.includes('已解决')) return { background: '#ecfccb', color: '#4d7c0f' };
  if (key.includes('closed') || key.includes('关闭')) return { background: '#e6edf3', color: '#334155' };
  if (key.includes('reopen') || key.includes('重新') || key.includes('reopened')) return { background: '#fff1f2', color: '#9f1239' };
  return { background: '#f3f4f6', color: '#374151' };
};

// date formatter
const formatDate = (v) => {
  if (!v) return '-';
  const d = new Date(v);
  if (isNaN(d.getTime())) return v;
  const Y = d.getFullYear();
  const M = String(d.getMonth() + 1).padStart(2, '0');
  const D = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  const s = String(d.getSeconds()).padStart(2, '0');
  return `${Y}-${M}-${D} ${h}:${m}:${s}`;
};

// interactions
const onProjectChange = async (val) => {
  state.selectedModuleId = null;
  await loadModules(val);
  await loadDefects(1);
};

const selectModule = (mOrNull) => {
  if (mOrNull === null) {
    state.selectedModuleId = null;
  } else {
    state.selectedModuleId = mOrNull.id ?? mOrNull._val;
  }
  loadDefects(1);
};

const isModuleSelected = (m) => String(state.selectedModuleId) === String(m.id ?? m._val);

const onSearch = () => { loadDefects(1); };

const resetFilters = async () => {
  state.q = '';
  state.selectedProjectId = null;
  state.selectedModuleId = null;
  await loadModules(null);
  await loadDefects(1);
};

const onSelectionChange = (rows) => { selectedRows.value = rows; };

const deleteSelected = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请选择数据');
    return;
  }
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除吗？', '确认', { type: 'warning' });
    const ids = selectedRows.value.map(r => r.id);
    await request.delete('/defect/deleteBatch', { data: ids });
    ElMessage.success('删除成功');
    await loadDefects(state.pageNum);
  } catch (err) {
    if (err !== 'cancel') {
      console.error(err);
      ElMessage.error('删除失败');
    }
  }
};

const confirmDelete = (row) => {
  confirmDialog.row = row;
  confirmDialog.visible = true;
};

const doDelete = async () => {
  try {
    await request.delete(`/defect/${confirmDialog.row.id}`);
    ElMessage.success('删除成功');
    confirmDialog.visible = false;
    await loadDefects(state.pageNum);
  } catch (err) {
    console.error(err);
    ElMessage.error('删除失败');
  }
};

const openAddDrawer = () => {
  drawer.isEdit = false;
  drawer.form = {
    id: null,
    title: '',
    description: '',
    projectId: state.selectedProjectId ?? null,
    moduleId: null,
    priorityId: state.priorities.length ? (state.priorities.find(p => /medium|m/i.test(p.name))?.id ?? state.priorities[0].id) : null,
    statusId: state.statuses.length ? (state.statuses.find(s => /new|新/i.test(s.name))?.id ?? state.statuses[0].id) : null,
    assignedTo: null,
  };
  drawer.modulesFiltered = state.modules.slice();
  drawer.visible = true;
};

const openEditDrawer = async (row) => {
  drawer.isEdit = true;
  const priorityId = row.priorityId ?? row.priority_id ?? (row.priorityName ? state.priorities.find(p => String(p.name).toLowerCase() === String(row.priorityName).toLowerCase())?.id : null);
  const statusId = row.statusId ?? row.status_id ?? (row.statusName ? state.statuses.find(s => String(s.name).toLowerCase() === String(row.statusName).toLowerCase())?.id : null);

  drawer.form = {
    id: row.id,
    title: row.title,
    description: row.description,
    projectId: row.projectId ?? row.project_id ?? null,
    moduleId: row.moduleId ?? row.module_id ?? null,
    priorityId: priorityId ?? null,
    statusId: statusId ?? null,
    assignedTo: row.assignedTo ?? row.assigned_to ?? null,
  };
  await loadModules(drawer.form.projectId);
  drawer.modulesFiltered = state.modules.slice();
  drawer.visible = true;
};

const onDrawerProjectChange = async (val) => {
  await loadModules(val);
  drawer.modulesFiltered = state.modules.slice();
};

const saveDrawer = async () => {
  try {
    if (!drawer.form.title || !drawer.form.title.trim()) {
      ElMessage.warning('请填写标题');
      return;
    }
    const payload = {
      ...drawer.form,
      projectId: drawer.form.projectId == null ? null : Number(drawer.form.projectId),
      moduleId: drawer.form.moduleId == null ? null : Number(drawer.form.moduleId),
      priorityId: drawer.form.priorityId == null ? null : Number(drawer.form.priorityId),
      statusId: drawer.form.statusId == null ? null : Number(drawer.form.statusId),
    };
    // include snake_case for backend compatibility
    payload.project_id = payload.projectId;
    payload.module_id = payload.moduleId;
    payload.priority_id = payload.priorityId;
    payload.status_id = payload.statusId;

    if (drawer.isEdit) {
      await request.put(`/defect/${payload.id}`, payload);
      ElMessage.success('更新成功');
    } else {
      await request.post('/defect', payload);
      ElMessage.success('创建成功');
    }
    drawer.visible = false;
    await loadDefects(1);
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

const handlePageChange = (page) => { loadDefects(page); };

onMounted(async () => {
  // load projects/modules/dictionaries/users first so mapping works immediately
  await loadProjects();
  await loadModules(null);
  await Promise.all([loadPriorities(), loadStatuses(), loadUsers()]);
  await loadDefects(1);
});
</script>

<style scoped>
/* 全局基础 */
.page-root {
  padding: 0;
  background: #f7fafc;
  min-height: 100vh;
  box-sizing: border-box;
}

/* 顶部导航 */
.top-links-bar {
  margin: 0;
  background: #ffffff;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(15,23,42,0.04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.top-links-inner {
  display: flex;
  gap: 16px;
  align-items: center;
}
.top-link {
  color: #6b7280;
  font-size: 13px;
  text-decoration: none;
  padding-bottom: 6px;
}
.top-link.active {
  color: #1f6feb;
  font-weight: 600;
  border-bottom: 2px solid #cfe6ff;
}

/* page container */
.page-container {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 顶部跨栏卡片 */
.top-card {
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  box-shadow: 0 6px 18px rgba(15,23,42,0.06);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  min-height: 56px;
  box-sizing: border-box;
}

/* 主行（左 + 右） */
.main-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
  width: 100%;
  box-sizing: border-box;
}

/* 左侧 card（15%） */
.left-card {
  flex: 0 0 15%;
  max-width: 320px;
  min-width: 180px;
  box-sizing: border-box;
  padding: 14px;
  display:flex;
  flex-direction:column;
  height: calc(100vh - 200px);
  overflow: auto;
}

/* 右侧 card（伸缩） */
.right-card {
  flex: 1 1 0;
  min-width: 0;
  display:flex;
  flex-direction:column;
  padding: 12px;
  box-sizing: border-box;
  max-height: calc(100vh - 160px);
}

/* card 公共 */
.card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 22px rgba(15,23,42,0.06);
}

/* left internals */
.left-card-header {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 10px;
}
.module-list {
  display:flex;
  flex-direction:column;
  gap: 10px;
}
.module-chip {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(107,114,128,0.06);
  color: #374151;
  cursor: pointer;
  transition: all .18s ease;
  font-size: 14px;
  user-select: none;
  border: 1px solid rgba(15,23,42,0.02);
}
.module-chip:hover { transform: translateY(-3px); box-shadow: 0 6px 18px rgba(15,23,42,0.04); }
.module-chip.active {
  background: rgba(51,65,85,0.12);
  color: #0f172a;
  border-color: rgba(15,23,42,0.06);
  font-weight: 600;
}

/* right toolbar */
.right-toolbar {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:8px;
  margin-bottom: 8px;
}
.total-text { color:#6b7280; font-size:13px; }

/* table viewport */
.table-viewport {
  overflow: auto;
  border-radius: 8px;
  padding-bottom: 8px;
  flex: 1 1 auto;
}

/* action links */
.action-link {
  display:inline-flex;
  align-items:center;
  gap:6px;
  padding:6px 8px;
  border-radius:6px;
  cursor:pointer;
  color:#1f6feb;
  font-weight:500;
}
.action-link:hover { background: rgba(31,111,235,0.06); }
.action-link.danger { color:#ef4444; }
.action-link.danger:hover { background: rgba(239,68,68,0.06); }

/* pagination */
.bottom-pagination {
  display:flex;
  justify-content:flex-end;
  align-items:center;
  padding-top: 8px;
}
::v-deep(.el-pagination) { font-size: 12px; }

/* drawer */
.drawer-custom { --drawer-bg: #ffffff; }
::v-deep(.drawer-custom .el-drawer__container) {
  background: var(--drawer-bg);
  box-shadow: -18px 0 40px rgba(22,27,34,0.12);
  border-left: 1px solid rgba(20,24,26,0.04);
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
}
.drawer-header { display:flex; align-items:center; justify-content:space-between; padding:14px 20px; border-bottom: 1px solid rgba(0,0,0,0.04); }
.drawer-title { font-size:16px; font-weight:600; color:#111827; }
.drawer-body { padding: 18px 28px 80px; max-height: calc(100% - 160px); overflow:auto; }
.drawer-footer { position:absolute; right:24px; bottom:18px; display:flex; gap:10px; z-index:30; }

/* responsive */
@media (max-width: 960px) {
  .main-row { flex-direction: column; }
  .left-card { flex: 0 0 auto; width: 100%; max-width: none; min-width: 0; height: auto; }
  .right-card { width: 100%; max-height: none; }
  .top-card { height: auto; padding: 12px; gap: 8px; flex-wrap:wrap; }
  .table-viewport { max-height: none; }
}
</style>

