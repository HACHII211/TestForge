<template>
  <div>
    <!-- 顶部导航条（无外层 margin）-->
    <div class="top-links-bar">
      <div class="top-links-inner">
        <a class="top-link active" href="/testforge/testcases">用例库</a>
        <a class="top-link" href="/testforge/testplans">测试计划</a>
        <a class="top-link" href="/testforge/reports">报告</a>
      </div>
    </div>

    <!-- 主卡片：搜索区 + 操作按钮 + 表格 + 分页 -->
    <div class="card merged-card">
      <!-- 查询与筛选行 -->
      <div class="toolbar">
        <el-input
            v-model="data.title"
            placeholder="请输入用例名称搜索"
            style="width: 320px"
            @keydown.enter="onSearch"
        ></el-input>

        <el-select v-model="data.filterProjectVal" placeholder="按项目筛选" clearable style="width: 240px; margin-left: 10px">
          <el-option v-for="p in data.projects" :key="p._val" :label="p.name" :value="p._val" />
        </el-select>

        <el-select v-model="data.filterModuleVal" placeholder="按模块筛选" clearable style="width: 220px; margin-left: 10px">
          <el-option v-for="m in data.modules" :key="m._val" :label="m.name" :value="m._val" />
        </el-select>

        <el-button type="primary" style="margin-left: 10px" @click="onSearch">查 询</el-button>
        <el-button type="success" style="margin-left: 10px" @click="restart">重 置</el-button>

        <div class="toolbar-right">
          <el-button type="danger" @click="delBatch">批量删除</el-button>
          <el-button type="primary" @click="handleAdd" style="margin-left: 10px">添加用例</el-button>
        </div>
      </div>

      <!-- 表格：只显示指定列 -->
      <el-table :data="data.case_list" @selection-change="handleSelectionChange" style="margin-top: 12px">
        <el-table-column type="selection" width="55"></el-table-column>

        <el-table-column label="用例ID" prop="id" width="90"></el-table-column>

        <el-table-column label="优先级" width="120">
          <template #default="scope">
            {{ scope.row.priority || scope.row.priorityName || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="用例状态" width="140">
          <template #default="scope">
            <el-tag v-if="scope.row.status === '新建'" :style="{ backgroundColor: '#E0F7FA', color: '#0097A7' }">新建</el-tag>
            <el-tag v-else-if="scope.row.status === '就绪'" :style="{ backgroundColor: '#E8F5E9', color: '#43A047' }">就绪</el-tag>
            <el-tag v-else-if="scope.row.status === '处理中'" :style="{ backgroundColor: '#FFF3E0', color: '#FB8C00' }">处理中</el-tag>
            <el-tag v-else-if="scope.row.status === '处理完成'" :style="{ backgroundColor: '#E8F5E9', color: '#2E7D32' }">处理完成</el-tag>
            <el-tag v-else-if="scope.row.status === '复测中'" :style="{ backgroundColor: '#E3F2FD', color: '#1E88E5' }">复测中</el-tag>
            <el-tag v-else-if="scope.row.status === '拒绝'" :style="{ backgroundColor: '#FFEBEE', color: '#E53935' }">拒绝</el-tag>
            <el-tag v-else-if="scope.row.status === '关闭'" :style="{ backgroundColor: '#ECEFF1', color: '#607D8B' }">关闭</el-tag>
            <span v-else>{{ scope.row.status ?? '-' }}</span>
          </template>
        </el-table-column>


        <el-table-column label="用例名称" >
          <template #default="scope">
            {{ scope.row.title || scope.row.name || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="所属项目" width="160">
          <template #default="scope">
            {{ getProjectName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column label="所属模块" width="160">
          <template #default="scope">
            {{ getModuleName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column label="创建人" width="140">
          <template #default="scope">
            {{ getUserName(scope.row, 'created') }}
          </template>
        </el-table-column>

        <el-table-column label="执行人" width="120">
          <template #default="scope">
            {{ getUserName(scope.row, 'executed') }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180">
          <template #default="scope">
            <span class="action-link" @click="handleUpdate(scope.row)"> 编辑</span>
            <span class="action-link danger" @click="handleDel(scope.row.id)"> 删除</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 12px;">
        <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="data.pageNum"
            :page-size="data.pageSize"
            :page-sizes="[10, 15, 20]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="data.total"
        />
      </div>
    </div>

    <!-- 删除确认 -->
    <el-dialog title="警告" v-model="data.formVisible3" style="text-align: center" width="460">
      <div style="text-align: center;margin: 40px;font-size: 18px">
        您确定要删除编号为 {{ data.deleteId }} 的用例吗？
      </div>
      <div style="margin-top: auto; display: flex; justify-content: flex-end; gap: 10px;">
        <el-button type="info" @click="data.formVisible3=false">取消</el-button>
        <el-button type="info" @click="deleteData">确认删除</el-button>
      </div>
    </el-dialog>

    <!-- 新增/编辑抽屉（替换原来的 dialog）-->
    <el-drawer
        v-model="data.updateFormVisible"
        title=""
        direction="rtl"
        :size="'50vw'"
        :with-header="false"
        class="drawer-custom"
        :destroy-on-close="true"
        append-to-body
        :lock-scroll="false"
        :style="{ transition: 'transform 1020ms cubic-bezier(0.25, 0.8, 0.25, 1)' }"
    >
      <!-- 自定义头部（看起来更扁平） -->
      <div class="drawer-header">
        <div class="drawer-title">{{ data.form.id ? '编辑用例' : '新增用例' }}</div>
        <div class="drawer-actions">
          <el-button type="text" @click="data.updateFormVisible = false">关闭</el-button>
        </div>
      </div>

      <div class="drawer-body">
        <div class="form-wrapper">
          <el-form :model="data.form" ref="formRef" label-width="120px">
            <el-form-item label="用例名称" prop="title">
              <el-input v-model="data.form.title" placeholder="请输入用例名称" style="width: 460px;" />
            </el-form-item>

            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="data.form.projectVal" placeholder="选择项目" style="width: 320px;" clearable @change="onProjectChange">
                <el-option v-for="p in data.projects" :key="p._val" :label="p.name" :value="p._val" />
              </el-select>
            </el-form-item>

            <el-form-item label="所属模块" prop="moduleId">
              <el-select v-model="data.form.moduleVal" placeholder="选择模块" style="width: 320px;" clearable>
                <el-option v-for="m in data.modulesFiltered" :key="m._val" :label="m.name" :value="m._val" />
              </el-select>
            </el-form-item>

            <el-form-item label="优先级" prop="priority">
              <el-select v-model="data.form.priority" placeholder="请选择优先级" style="width: 320px;">
                <el-option label="高" value="High" />
                <el-option label="中" value="Medium" />
                <el-option label="低" value="Low" />
              </el-select>
            </el-form-item>

            <el-form-item label="状态" prop="status">
              <el-select v-model="data.form.status" placeholder="请选择状态" style="width: 320px;">
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
              <el-select v-model="data.form.executedByVal" placeholder="选择用户" style="width: 320px;" clearable>
                <el-option v-for="u in data.users" :key="u._val" :label="u.username || u.name || u.userName" :value="u._val" />
              </el-select>
            </el-form-item>

            <el-form-item label="前置条件" prop="preCondition">
              <el-input type="textarea" v-model="data.form.preCondition" placeholder="前置条件" style="width: 100%" />
            </el-form-item>

            <el-form-item label="执行步骤" prop="steps">
              <el-input type="textarea" v-model="data.form.steps" placeholder="执行步骤" style="width: 100%" />
            </el-form-item>

            <el-form-item label="预期结果" prop="expectedResult">
              <el-input type="textarea" v-model="data.form.expectedResult" placeholder="预期结果" style="width: 100%" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 固定底部按钮 -->
      <div class="drawer-footer">
        <el-button type="danger" @click="data.updateFormVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit } from '@element-plus/icons-vue';

const data = reactive({
  title: null,
  filterProjectVal: null,
  filterModuleVal: null,
  pageNum: 1,
  pageSize: 10,
  total: 0,
  case_list: [],
  all_cases: [],
  deleteId: null,
  updateFormVisible: false,
  formVisible3: false,
  form: {},
  ids: [],
  projects: [],
  modules: [],
  modulesFiltered: [],
  users: [],
});

// ref for form
const formRef = ref(null);

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

// 宽容的字段获取器
const getProjectName = (row) => {
  if (!row) return '-';
  return row.projectName || row.project_name || (row.project && (row.project.name || row.project.projectName)) || '-';
};

const getModuleName = (row) => {
  if (!row) return '-';
  return row.moduleName || row.module_name || (row.module && (row.module.name || row.module.moduleName)) || '-';
};

const getUserName = (row, kind = 'created') => {
  if (!row) return '-';
  // kind: 'created' or 'executed'
  const created = row.createdByName || row.created_by_name || row.createdBy || row.created_by || (row.createdByName);
  const executed = row.executedByName || row.executed_by_name || row.executedBy || row.executed_by || row.executorName;
  if (kind === 'created') return created || '-';
  return executed || '-';
};

// 构建下拉数据（去重）
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
  data.projects = Array.from(pMap.values());
  data.modules = Array.from(mMap.values());
  data.users = Array.from(uMap.values());
};

// 本地过滤并分页（当使用按项目/模块筛选时启用）
const applyFiltersAndPaginate = (list, pageNum, pageSize) => {
  let filtered = list.slice();
  if (data.filterProjectVal) {
    filtered = filtered.filter(c => {
      const id = c.projectId ?? c.project_id ?? (c.project && c.project.id);
      const candidate = id != null ? String(id) : `name:${c.projectName ?? c.project_name}`;
      return candidate === data.filterProjectVal;
    });
  }
  if (data.filterModuleVal) {
    filtered = filtered.filter(c => {
      const id = c.moduleId ?? c.module_id ?? (c.module && c.module.id);
      const candidate = id != null ? String(id) : `name:${c.moduleName ?? c.module_name}`;
      return candidate === data.filterModuleVal;
    });
  }
  data.total = filtered.length;
  const start = (pageNum - 1) * pageSize;
  return filtered.slice(start, start + pageSize);
};

// 加载数据：当使用按项目/模块筛选时采用一次性拉取全部并在客户端过滤，否则按分页请求
const load = async () => {
  try {
    if (data.filterProjectVal || data.filterModuleVal) {
      const allResp = await request.get('/testcases', { params: { pageNum: 1, pageSize: 100000 } });
      const body = allResp.data || {};
      const list = body.list || (body.data && body.data.list) || [];
      data.all_cases = list;
      buildListsFromCases(list);
      data.case_list = applyFiltersAndPaginate(list, data.pageNum, data.pageSize);
    } else {
      const resp = await request.get('/testcases', { params: { title: data.title, pageNum: data.pageNum, pageSize: data.pageSize } });
      const body = resp.data || {};
      const list = body.list || (body.data && body.data.list) || [];
      data.case_list = list;
      data.total = body.total || (body.data && body.data.total) || list.length;
      buildListsFromCases(list);
    }
  } catch (err) {
    console.error(err);
    ElMessage.error('加载用例列表失败');
    data.case_list = [];
    data.total = 0;
  }
};

const onSearch = () => {
  data.pageNum = 1;
  load();
};

const restart = () => {
  data.title = null;
  data.filterProjectVal = null;
  data.filterModuleVal = null;
  data.pageNum = 1;
  data.pageSize = 10;
  load();
};

const handleAdd = () => {
  data.form = {
    title: '',
    projectVal: null,
    moduleVal: null,
    priority: '中',
    status: '新建',
    createdByVal: null,
    executedByVal: null,
    preCondition: '',
    steps: '',
    expectedResult: ''
  };
  data.modulesFiltered = [];
  data.updateFormVisible = true;
};

const preparePayload = (form) => {
  const payload = { ...form };
  // 处理 project/module/user 下拉值（支持 id 或 name:xxx）
  if (payload.projectVal == null) {
    payload.projectId = null;
  } else if (String(payload.projectVal).startsWith('name:')) {
    payload.projectId = null;
  } else {
    payload.projectId = Number(payload.projectVal);
  }
  if (payload.moduleVal == null) {
    payload.moduleId = null;
  } else if (String(payload.moduleVal).startsWith('name:')) {
    payload.moduleId = null;
  } else {
    payload.moduleId = Number(payload.moduleVal);
  }
  if (payload.createdByVal == null) {
    payload.createdBy = null;
  } else if (String(payload.createdByVal).startsWith('name:')) {
    payload.createdBy = null;
  } else {
    payload.createdBy = Number(payload.createdByVal);
  }
  if (payload.executedByVal == null) {
    payload.executedBy = null;
  } else if (String(payload.executedByVal).startsWith('name:')) {
    payload.executedBy = null;
  } else {
    payload.executedBy = Number(payload.executedByVal);
  }

  // 将前端字段名映射为后端实体字段（根据你的后端字段名调整）
  payload.title = payload.title;
  payload.preCondition = payload.preCondition;
  payload.steps = payload.steps;
  payload.expectedResult = payload.expectedResult;
  payload.priority = payload.priority;
  payload.status = payload.status;
  delete payload.projectVal;
  delete payload.moduleVal;
  delete payload.createdByVal;
  delete payload.executedByVal;
  return payload;
};

const save = async () => {
  if (!data.form.title) {
    ElMessage.warning('请填写用例名称');
    return;
  }
  try {
    const payload = preparePayload(data.form);
    if (data.form.id) {
      await request.put(`/testcases/${data.form.id}`, payload);
      ElMessage.success('更新成功');
    } else {
      await request.post('/testcases', payload);
      ElMessage.success('添加成功');
    }
    data.updateFormVisible = false;
    data.pageNum = 1;
    await load();
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

const handleDel = (id) => {
  data.formVisible3 = true;
  data.deleteId = id;
};

const deleteData = async () => {
  try {
    await request.delete(`/testcases/${data.deleteId}`);
    ElMessage.success('删除成功');
    data.formVisible3 = false;
    await load();
  } catch (err) {
    console.error(err);
    ElMessage.error('删除失败');
  } finally {
    data.deleteId = null;
  }
};

const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row || {}));
  // fill dropdown helper vals
  const pid = data.form.projectId ?? data.form.project_id ?? (data.form.project && data.form.project.id);
  const pname = data.form.projectName ?? data.form.project_name ?? (data.form.project && data.form.project.name);
  data.form.projectVal = pid != null ? String(pid) : (pname ? `name:${pname}` : null);

  const mid = data.form.moduleId ?? data.form.module_id ?? (data.form.module && data.form.module.id);
  const mname = data.form.moduleName ?? data.form.module_name ?? (data.form.module && data.form.module.name);
  data.form.moduleVal = mid != null ? String(mid) : (mname ? `name:${mname}` : null);

  const createdId = data.form.createdBy ?? data.form.created_by ?? (data.form.createdBy && data.form.createdBy.id);
  const createdName = data.form.createdByName ?? data.form.created_by_name ?? (data.form.createdBy && (data.form.createdBy.username || data.form.createdBy.name));
  data.form.createdByVal = createdId != null ? String(createdId) : (createdName ? `name:${createdName}` : null);

  const execId = data.form.executedBy ?? data.form.executed_by ?? (data.form.executedBy && data.form.executedBy.id);
  const execName = data.form.executedByName ?? data.form.executed_by_name ?? data.form.executorName;
  data.form.executedByVal = execId != null ? String(execId) : (execName ? `name:${execName}` : null);

  // filter modules by project
  filterModulesByProject(data.form.projectVal);
  data.updateFormVisible = true;
};

const handleSelectionChange = (rows) => {
  data.ids = rows.map((row) => row.id);
};

const delBatch = async () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据');
    return;
  }
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除吗？', '确认', { type: 'warning' });
    await request.delete('/testcases/deleteBatch', { data: data.ids });
    ElMessage.success('删除成功');
    await load();
  } catch (err) {
    if (err !== 'cancel') console.error(err);
  }
};

const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1;
  load();
};

const handleCurrentChange = (page) => {
  data.pageNum = page;
  load();
};

const onProjectChange = (val) => {
  filterModulesByProject(val);
};

const filterModulesByProject = (projectVal) => {
  if (!projectVal) {
    data.modulesFiltered = data.modules.slice();
    return;
  }
  // modules may carry projectId property we set earlier in buildLists
  data.modulesFiltered = data.modules.filter(m => {
    // m.projectId may be null, projectVal may be name:xxx
    if (String(projectVal).startsWith('name:')) {
      return String(m._val).startsWith('name:');
    }
    return String(m.projectId) === String(projectVal) || String(m._val) === String(projectVal);
  });
};

// 初始加载
load();
</script>

<style scoped>
/* 顶部条：白色背景、无外边距 */
.top-links-bar {
  margin: 0;
  background: #ffffff;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  -webkit-tap-highlight-color: transparent;
}

/* 内部容器：左对齐，平铺排列 */
.top-links-inner {
  display: flex;
  justify-content: flex-start;
  gap: 18px;
  align-items: center;
  padding-left: 12px;
}

.top-link {
  color: #6b7280;
  text-decoration: none;
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;
  padding: 6px 0;
  transition: color 0.15s ease, border-color 0.15s ease;
  border-bottom: 2px solid transparent;
  -webkit-tap-highlight-color: transparent;
  cursor: pointer;
}
.top-link:hover,
.top-link:focus {
  color: #4b5563;
  text-decoration: none;
  outline: none;
  box-shadow: none;
}
.top-link.selected,
.top-link.active {
  color: #1f6feb;
  font-weight: 500;
  border-bottom-color: #1f6feb;
  padding-bottom: 4px;
}

/* 合并后的卡片样式 */
.merged-card {
  margin: 5px;
  background-color: white;
  padding: 10px;
  box-shadow: 0 0 12px rgba(0,0,0,.12);
}

/* 工具条：左侧筛选，右侧操作 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

/* 表单弹窗内边距 */
.form-wrapper {
  padding: 10px 20px;
}

.action-link {
  display: inline-block;
  margin-right: 12px;
  cursor: pointer;
  color: #1f6feb;
  font-weight: 500;
  font-size: 14px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background-color .12s ease, color .12s ease;
}
.action-link:hover {
  background-color: rgba(31,111,235,0.08);
}
.action-link.danger {
  color: #ef4444;
}
.action-link.danger:hover {
  background-color: rgba(239,68,68,0.08);
}

/* ========== Drawer 自定义样式 ========== */
/* custom class applied to el-drawer root */
.drawer-custom {
  /* remove default border radius so it "attaches" to the browser edge cleanly */
  --drawer-bg: #ffffff;
}

/* 深度选择器，调整内部 body 与头部样式 */
::v-deep(.drawer-custom .el-drawer__container) {
  background: var(--drawer-bg);
  box-shadow: -18px 0 40px rgba(22,27,34,0.12);
  border-left: 1px solid rgba(20,24,26,0.04);
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
  overflow: hidden;
}

/* header area we inserted */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  background: linear-gradient(180deg, rgba(255,255,255,0.9), rgba(250,250,250,0.9));
}
.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.drawer-actions {
  display: flex;
  gap: 8px;
}

/* body: scrollable content area */
.drawer-body {
  height: calc(100% - 120px); /* leave space for header + footer */
  overflow: auto;
  padding-bottom: 16px;
}

/* footer: fixed at bottom of drawer */
.drawer-footer {
  position: absolute;
  right: 24px;
  bottom: 18px;
  display: flex;
  gap: 10px;
  z-index: 30;
}

/* form wrapper padding override to look nicer in drawer */
.drawer-custom .form-wrapper {
  padding: 18px 28px 80px 28px; /* leave bottom space for floating footer */
}

/* smaller inputs area adjustments */
::v-deep(.drawer-custom .el-input__inner),
::v-deep(.drawer-custom .el-select .el-input__inner) {
  font-size: 14px;
}

/* ensure drawer is attached to right edge (no outer margin) */
::v-deep(.el-drawer__mask) {
  /* slightly darker backdrop for emphasis */
  background-color: rgba(13,16,20,0.36);
}

/* smooth slide animation tweak (element-plus has its own, this enhances) */
::v-deep(.el-drawer__container) {
  transition: transform 320ms cubic-bezier(.2,.9,.3,1);
}

/* responsive: on very small screens make it full width */
@media (max-width: 720px) {
  ::v-deep(.el-drawer__container) {
    width: 100% !important;
    border-radius: 0 !important;
  }
  .drawer-body { height: calc(100% - 120px); }
}
</style>
