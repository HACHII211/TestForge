<template>
  <div class="page-root">
<!--    <div class="top-links-bar">-->
<!--      <div class="top-links-inner">-->
<!--        <a class="top-link active" href="/testforge/testcases">用例库</a>-->
<!--      </div>-->
<!--    </div>-->

    <div class="page-container">
      <div class="card top-card" ref="topCard">
        <div class="top-left">
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
                :value="p.id ?? p._val"
            />
          </el-select>
        </div>

        <div class="top-right">
          <el-input
              v-model="state.q"
              placeholder="按用例名称搜索（回车查询）"
              @keydown.enter.native="onSearch"
              clearable
              :style="{ width: 'min(40vw, 420px)' }"
          />
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="main-row">
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

        <div class="card right-card">
          <div class="right-toolbar">
            <div class="left-actions">
              <el-button type="danger" @click="deleteSelected" :disabled="!selectedRows.length" v-if="hasPermission('testcase_manage')">批量删除</el-button>
              <el-button type="primary" @click="openAddDrawer" style="margin-left: 8px" v-if="hasPermission('testcase_manage')">新增用例</el-button>
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
              <el-table-column type="selection" width="55" v-if="hasPermission('testcase_manage')"/>
              <el-table-column prop="id" label="用例ID" width="90" />
              <el-table-column label="优先级" width="120">
                <template #default="scope">
                  <el-tag :type="priorityTagType(resolvePriorityName(scope.row))">
                    {{ resolvePriorityName(scope.row) || '-' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="用例状态" width="140">
                <template #default="scope">
                  <el-tag :style="statusTagStyle(resolveStatusName(scope.row))">
                    {{ resolveStatusName(scope.row) || scope.row.status || '-' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="用例名称" min-width="260">
                <template #default="scope">
                  {{ scope.row.title || scope.row.name || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="所属项目" prop="projectName" width="160">
                <template #default="scope">
                  {{ getProjectName(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column label="所属模块" prop="moduleName" width="160">
                <template #default="scope">
                  {{ getModuleName(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column label="创建人" prop="createdByName" width="140">
                <template #default="scope">
                  {{ getUserName(scope.row, 'created') }}
                </template>
              </el-table-column>
              <el-table-column label="执行人" prop="executedByName" width="120">
                <template #default="scope">
                  {{ getUserName(scope.row, 'executed') }}
                </template>
              </el-table-column>
              <el-table-column label="创建时间" prop="createdAt" width="160">
                <template #default="scope">
                  {{ formatDate(scope.row.createdAt || scope.row.created_at) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right" v-if="hasPermission('testcase_manage')">
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

    <el-dialog title="确认删除" v-model="confirmDialog.visible" width="460">
      <div style="text-align:center; font-size:16px; margin: 24px 0;">
        确认删除用例：<strong>{{ confirmDialog.row?.id }} - {{ confirmDialog.row?.title }}</strong> ?
      </div>
      <span slot="footer" class="dialog-footer">
    <el-button @click="confirmDialog.visible = false">取消</el-button>
    <el-button type="danger" @click="doDelete">确认删除</el-button>
  </span>
    </el-dialog>

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
        <div class="drawer-title">{{ drawer.isEdit ? '编辑用例' : '新增用例' }}</div>
        <div class="drawer-actions">
          <el-button type="text" @click="drawer.visible = false">关闭</el-button>
        </div>
      </div>

      <div class="drawer-body">
        <el-form :model="drawer.form" ref="drawerFormRef" label-width="110px">
          <el-form-item label="用例名称" prop="title">
            <el-input v-model="drawer.form.title" />
          </el-form-item>

          <el-form-item label="所属项目" prop="projectId">
            <el-select v-model="drawer.form.projectId" placeholder="选择项目" @change="onDrawerProjectChange" clearable>
              <el-option v-for="p in state.projects" :key="p.id ?? p._val" :label="p.name" :value="p.id ?? p._val" />
            </el-select>
          </el-form-item>

          <el-form-item label="所属模块" prop="moduleId">
            <el-select v-model="drawer.form.moduleId" placeholder="选择模块" clearable>
              <el-option
                  v-for="m in drawer.modulesFiltered"
                  :key="m.id ?? m._val"
                  :label="getModuleDisplayName(m)"
                  :value="m.id ?? m._val"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="优先级" prop="priority">
            <el-select v-model="drawer.form.priority" placeholder="请选择优先级" clearable>
              <el-option label="高" value="High" />
              <el-option label="中" value="Medium" />
              <el-option label="低" value="Low" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态" prop="status">
            <el-select v-model="drawer.form.status" placeholder="请选择状态" clearable>
              <el-option label="新建" value="新建" />
              <el-option label="就绪" value="就绪" />
              <el-option label="处理中" value="处理中" />
              <el-option label="处理完成" value="处理完成" />
              <el-option label="复测中" value="复测中" />
              <el-option label="拒绝" value="拒绝" />
              <el-option label="关闭" value="关闭" />
            </el-select>
          </el-form-item>

          <el-form-item label="执行人" prop="executedBy">
            <el-select v-model="drawer.form.executedBy" clearable>
              <el-option v-for="u in state.users" :key="u.id ?? u._val" :label="u.username" :value="u.id ?? u._val" />
            </el-select>
          </el-form-item>

          <el-form-item label="前置条件" prop="preCondition">
            <el-input type="textarea" v-model="drawer.form.preCondition" rows="4" />
          </el-form-item>

          <el-form-item label="执行步骤" prop="steps">
            <el-input type="textarea" v-model="drawer.form.steps" rows="6" />
          </el-form-item>

          <el-form-item label="预期结果" prop="expectedResult">
            <el-input type="textarea" v-model="drawer.form.expectedResult" rows="4" />
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
  selectedProjectId: null,
  selectedModuleId: null,
  q: '',
  pageNum: 1,
  pageSize: 10,
  total: 0,
  case_list: [],
  all_cases: [],
});

const selectedRows = ref([]);
const drawerFormRef = ref(null);

const drawer = reactive({
  visible: false,
  isEdit: false,
  form: {
    id: null,
    title: '',
    projectId: null,
    moduleId: null,
    priority: null,
    status: null,
    executedBy: null,
    preCondition: '',
    steps: '',
    expectedResult: ''
  },
  modulesFiltered: [],
});

const confirmDialog = reactive({
  visible: false,
  row: null,
});

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

const loadProjects = async () => {
  try {
    const resp = await request.get('/projects', { params: { pageNum: 1, pageSize: 200 } });
    const list = extractList(resp);
    state.projects = list.map(p => ({ id: p.id, name: p.name, _val: p.id }));
  } catch (err) {
    console.error(err);
    state.projects = [];
  }
};

const loadModules = async (projectId = null) => {
  try {
    const params = { pageNum: 1, pageSize: 200 };
    if (projectId != null) params.projectId = projectId;
    const resp = await request.get('/modules', { params });
    const list = extractList(resp);
    state.modules = list.map(m => ({
      id: m.id,
      name: m.name,
      projectId: m.projectId ?? m.project_id ?? null,
      _val: m.id
    }));
  } catch (err) {
    console.error(err);
    state.modules = [];
  }
};

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

const normalizeCaseRow = (r) => {
  const row = { ...r };
  row.projectId = row.projectId ?? row.project_id ?? null;
  row.moduleId = row.moduleId ?? row.module_id ?? null;
  row.priority = row.priority ?? row.priorityName ?? row.priority_name ?? null;
  row.status = row.status ?? row.statusName ?? row.status_name ?? null;
  const createdId = row.createdBy ?? row.created_by ?? null;
  const executedId = row.executedBy ?? row.executed_by ?? null;
  row.createdBy = createdId;
  row.executedBy = executedId;
  row.createdByName = row.createdByName ?? row.created_by_name ?? (createdId ? findUserNameById(createdId) : null) ?? null;
  row.executedByName = row.executedByName ?? row.executed_by_name ?? (executedId ? findUserNameById(executedId) : null) ?? null;
  row.projectName = row.projectName ?? row.project_name ?? (row.project && (row.project.name || row.project.projectName)) ?? null;
  row.moduleName = row.moduleName ?? row.module_name ?? (row.module && (row.module.name || row.module.moduleName)) ?? null;
  return row;
};

const loadCases = async (pageNum = 1, pageSize = state.pageSize) => {
  try {
    if (state.selectedProjectId != null || state.selectedModuleId != null || state.q) {
      const params = { pageNum, pageSize, title: state.q };
      if (state.selectedProjectId != null) params.projectId = state.selectedProjectId;
      if (state.selectedModuleId != null) params.moduleId = state.selectedModuleId;
      const resp = await request.get('/testcases', { params });
      const pi = extractPageInfo(resp);
      state.case_list = pi.list.map(normalizeCaseRow);
      state.total = pi.total;
      state.pageNum = pageNum;
      state.pageSize = pageSize;
    } else {
      const resp = await request.get('/testcases', { params: { title: state.q, pageNum, pageSize } });
      const pi = extractPageInfo(resp);
      state.case_list = pi.list.map(normalizeCaseRow);
      state.total = pi.total;
      state.pageNum = pageNum;
      state.pageSize = pageSize;
    }
    buildListsFromCases(state.case_list);
  } catch (err) {
    console.error(err);
    ElMessage.error('加载用例失败');
    state.case_list = [];
    state.total = 0;
  }
};

const tableData = computed(() => state.case_list);

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

const resolvePriorityName = (row) => {
  if (!row) return null;
  return row.priority || null;
};
const resolveStatusName = (row) => {
  if (!row) return null;
  return row.status || null;
};

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
const getUserName = (row, kind = 'created') => {
  if (!row) return '-';
  const created = row.createdByName || row.created_by_name || row.createdBy || row.created_by;
  const executed = row.executedByName || row.executed_by_name || row.executedBy || row.executed_by || row.executorName;
  if (kind === 'created') return created || '-';
  return executed || '-';
};

const getModuleDisplayName = (m) => {
  if (!m) return '';
  if (state.selectedProjectId == null) {
    const pjName = findProjectName(m.projectId);
    return pjName ? `${m.name} (${pjName})` : m.name;
  }
  return m.name;
};

const priorityTagType = (pName) => {
  if (!pName) return 'info';
  const key = String(pName).toLowerCase();
  if (key.includes('high') || key.includes('h') || key === 'high' || key.includes('高')) return 'danger';
  if (key.includes('medium') || key.includes('m') || key === 'medium' || key.includes('中')) return 'warning';
  if (key.includes('low') || key.includes('l') || key === 'low' || key.includes('低')) return 'success';
  return 'info';
};

const statusTagStyle = (s) => {
  if (!s) return { background: '#f3f4f6', color: '#374151' };
  const key = String(s).toLowerCase();
  if (key.includes('新') || key.includes('new')) return { background: '#e0f2fe', color: '#0369a1' };
  if (key.includes('就绪') || key.includes('ready')) return { background: '#ecfccb', color: '#4d7c0f' };
  if (key.includes('处') || key.includes('progress') || key.includes('处理中')) return { background: '#fff7ed', color: '#b45309' };
  if (key.includes('完成') || key.includes('resolved') || key.includes('处理完成')) return { background: '#ecfccb', color: '#4d7c0f' };
  if (key.includes('复测') || key.includes('retest')) return { background: '#e3f2fd', color: '#1e88e5' };
  if (key.includes('拒绝') || key.includes('rejected')) return { background: '#fff1f2', color: '#9f1239' };
  if (key.includes('关') || key.includes('closed')) return { background: '#e6edf3', color: '#334155' };
  return { background: '#f3f4f6', color: '#374151' };
};

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

const buildListsFromCases = (list) => {
  const pMap = new Map();
  const mMap = new Map();
  const uMap = new Map();
  list.forEach(c => {
    const pid = c.projectId ?? c.project_id ?? (c.project && c.project.id);
    const pname = c.projectName ?? c.project_name ?? (c.project && c.project.name);
    if (pid != null || pname) {
      const _val = pid != null ? String(pid) : `name:${pname}`;
      if (!pMap.has(_val)) pMap.set(_val, { id: pid ?? null, name: pname ?? '未知', _val });
    }
    const mid = c.moduleId ?? c.module_id ?? (c.module && c.module.id);
    const mname = c.moduleName ?? c.module_name ?? (c.module && c.module.name);
    if (mid != null || mname) {
      const _val = mid != null ? String(mid) : `name:${mname}`;
      if (!mMap.has(_val)) mMap.set(_val, { id: mid ?? null, name: mname ?? '未知', _val, projectId: pid });
    }
    const createdId = c.createdBy ?? c.created_by ?? (c.createdBy && c.createdBy.id);
    const createdName = c.createdByName ?? c.created_by_name ?? (c.createdBy && c.createdBy.username) ?? c.created_by_name;
    if (createdId != null || createdName) {
      const _val = createdId != null ? String(createdId) : `name:${createdName}`;
      if (!uMap.has(_val)) uMap.set(_val, { id: createdId ?? null, username: createdName ?? '未知', _val });
    }
    const execId = c.executedBy ?? c.executed_by ?? (c.executedBy && c.executedBy.id);
    const execName = c.executedByName ?? c.executed_by_name ?? (c.executedBy && c.executedBy.username) ?? c.executorName;
    if (execId != null || execName) {
      const _val = execId != null ? String(execId) : `name:${execName}`;
      if (!uMap.has(_val)) uMap.set(_val, { id: execId ?? null, username: execName ?? '未知', _val });
    }
  });
  state.projects = Array.from(pMap.values());
  state.modules = Array.from(mMap.values());
  state.users = Array.from(uMap.values());
};

const onProjectChange = async (val) => {
  state.selectedModuleId = null;
  await loadModules(val);
  await loadCases(1);
};

const selectModule = (mOrNull) => {
  if (mOrNull === null) {
    state.selectedModuleId = null;
  } else {
    state.selectedModuleId = mOrNull.id ?? mOrNull._val;
  }
  loadCases(1);
};

const isModuleSelected = (m) => String(state.selectedModuleId) === String(m.id ?? m._val);

const onSearch = () => { state.pageNum = 1; loadCases(1); };

const resetFilters = async () => {
  state.q = '';
  state.selectedProjectId = null;
  state.selectedModuleId = null;
  await loadModules(null);
  await loadCases(1);
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
    await request.delete('/testcases/deleteBatch', { data: ids });
    ElMessage.success('删除成功');
    await loadCases(state.pageNum);
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
    await request.delete(`/testcases/${confirmDialog.row.id}`);
    ElMessage.success('删除成功');
    confirmDialog.visible = false;
    await loadCases(state.pageNum);
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
    projectId: state.selectedProjectId ?? null,
    moduleId: null,
    priority: 'Medium',
    status: '新建',
    executedBy: null,
    preCondition: '',
    steps: '',
    expectedResult: ''
  };
  drawer.modulesFiltered = state.modules.slice();
  drawer.visible = true;
};

const openEditDrawer = async (row) => {
  drawer.isEdit = true;
  drawer.form = {
    id: row.id,
    title: row.title,
    projectId: row.projectId ?? row.project_id ?? null,
    moduleId: row.moduleId ?? row.module_id ?? null,
    priority: row.priority ?? null,
    status: row.status ?? null,
    executedBy: row.executedBy ?? row.executed_by ?? null,
    preCondition: row.preCondition ?? row.pre_condition ?? '',
    steps: row.steps ?? '',
    expectedResult: row.expectedResult ?? row.expected_result ?? ''
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
      ElMessage.warning('请填写用例名称');
      return;
    }
    const payload = {
      ...drawer.form,
      projectId: drawer.form.projectId == null ? null : Number(drawer.form.projectId),
      moduleId: drawer.form.moduleId == null ? null : Number(drawer.form.moduleId),
    };
    payload.project_id = payload.projectId;
    payload.module_id = payload.moduleId;

    if (drawer.isEdit) {
      await request.put(`/testcases/${payload.id}`, payload);
      ElMessage.success('更新成功');
    } else {
      await request.post('/testcases', payload);
      ElMessage.success('创建成功');
    }
    drawer.visible = false;
    await loadCases(1);
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

const handlePageChange = (page) => { loadCases(page); };

onMounted(async () => {
  await loadProjects();
  await loadModules(null);
  await loadUsers();
  await loadCases(1);
});
</script>

<style scoped>
.page-root {
  padding: 0;
  background: #f7fafc;
  min-height: 100vh;
  box-sizing: border-box;
}
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
.page-container {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
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
.main-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
  width: 100%;
  box-sizing: border-box;
}
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
.right-card {
  flex: 1 1 0;
  min-width: 0;
  display:flex;
  flex-direction:column;
  padding: 12px;
  box-sizing: border-box;
  max-height: calc(100vh - 160px);
}
.card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 22px rgba(15,23,42,0.06);
}
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
.right-toolbar {
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:8px;
  margin-bottom: 8px;
}
.total-text { color:#6b7280; font-size:13px; }
.table-viewport {
  overflow: auto;
  border-radius: 8px;
  padding-bottom: 8px;
  flex: 1 1 auto;
}
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
.bottom-pagination {
  display:flex;
  justify-content:flex-end;
  align-items:center;
  padding-top: 8px;
}
::v-deep(.el-pagination) { font-size: 12px; }
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
@media (max-width: 960px) {
  .main-row { flex-direction: column; }
  .left-card { flex: 0 0 auto; width: 100%; max-width: none; min-width: 0; height: auto; }
  .right-card { width: 100%; max-height: none; }
  .top-card { height: auto; padding: 12px; gap: 8px; flex-wrap:wrap; }
  .table-viewport { max-height: none; }
}
</style>
