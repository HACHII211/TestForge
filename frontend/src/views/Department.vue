<template>
  <div>
    <!-- 顶部导航 -->
    <div class="top-links-bar">
      <div class="top-links-inner">
        <a class="top-link" href="/testforge/organization">
          {{ hasPermission('project_manage') ? '组织管理' : '组织' }}
        </a>
        <a class="top-link active" href="/testforge/department">
          {{ hasPermission('project_manage') ? '部门管理' : '部门' }}
        </a>
        <a class="top-link" href="/testforge/permission">
          {{ hasPermission('project_manage') ? '权限管理' : '权限' }}
        </a>
      </div>
    </div>

    <div class="card merged-card">
      <!-- 工具条 -->
      <div class="toolbar">
        <el-input
            v-model="data.name"
            placeholder="请输入部门名称搜索"
            style="width: 300px"
            @keydown.enter="onSearch"
        ></el-input>

        <el-button type="primary" style="margin-left: 10px" @click="onSearch">查 询</el-button>
        <el-button type="success" style="margin-left: 10px" @click="restart">重 置</el-button>

        <div class="toolbar-right">
          <el-button type="danger" @click="delBatch" v-if="hasPermission('user_manage')">批量删除</el-button>
          <el-button type="primary" @click="handleAdd" style="margin-left: 10px" v-if="hasPermission('user_manage')">添加</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="data.dept_list" @selection-change="handleSelectionChange" style="margin-top: 12px">
        <el-table-column type="selection" width="55" ></el-table-column>

        <el-table-column label="部门名称" prop="name" min-width="120">
          <template #default="scope">
            {{ scope.row.name || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="上级部门" prop="parentName" width="180">
          <template #default="scope">
            {{ scope.row.parentName || '-' }}
          </template>
        </el-table-column>

        <!-- 成员列 -->
        <el-table-column label="成员" min-width="260">
          <template #default="scope">
            <div
                class="members-cell"
                @mouseenter="onMembersEnter($event, scope.row)"
                @mouseleave="onMembersLeave"
                tabindex="0"
                @focus="onMembersEnter($event, scope.row)"
                @blur="onMembersLeave"
            >
              <span class="members-inline">
                {{ joinMemberNames(scope.row) }}
              </span>

              <teleport to="body">
                <transition name="panel-fade" appear>
                  <div
                      v-if="panel.visible && panel.deptId === scope.row.id"
                      class="members-panel"
                      :style="panel.style"
                      @mouseenter="onPanelEnter"
                      @mouseleave="onPanelLeave"
                  >
                    <div class="panel-header">
                      <span class="panel-title">{{ scope.row.name }} 成员</span>
                      <span class="panel-count">共 {{ (membersMap.get(scope.row.id) || []).length }} 人</span>
                    </div>
                    <div class="panel-list">
                      <div v-if="(membersMap.get(scope.row.id) || []).length === 0" class="panel-empty">
                        暂无成员
                      </div>
                      <div v-else>
                        <div v-for="u in membersMap.get(scope.row.id)" :key="u.id" class="panel-item">
                          <div class="panel-item-name">{{ u.username || u.userName || u.name || ('ID:' + u.id) }}</div>
                          <div class="panel-item-email" v-if="u.email">{{ u.email }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </transition>
              </teleport>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="描述" prop="description" min-width="240">
          <template #default="scope">
            <span class="desc-ellipsis">{{ scope.row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.created_at || scope.row.createdAt) }}
          </template>
        </el-table-column>

        <!-- 操作列：改为纯文字风格 -->
        <el-table-column label="操作" width="160" v-if="hasPermission('user_manage')">
          <template #default="scope">
            <span class="action-link" @click="handleUpdate(scope.row)">编辑</span>
            <span class="action-link danger" @click="handleDel(scope.row.id)">删除</span>
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
    <el-dialog title="警告" v-model="data.formVisible3" style="text-align: center" width="450">
      <div style="text-align: center;margin: 40px;font-size: 18px">
        您确定要删除部门 “{{ data.deleteName }}” 吗？
      </div>
      <div style="margin-top: auto; display: flex; justify-content: flex-end; gap: 10px;">
        <el-button type="info" @click="data.formVisible3=false">取消</el-button>
        <el-button type="info" @click="deleteData">确认删除</el-button>
      </div>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog title="部门信息" v-model="data.updateFormVisible" width="600" destroy-on-close>
      <div class="form-wrapper">
        <el-form :model="data.form" ref="formRef" label-width="120px">
          <el-form-item label="部门名称" prop="name">
            <el-input v-model="data.form.name" placeholder="请输入部门名称" style="width: 420px;" />
          </el-form-item>

          <el-form-item label="上级部门" prop="parentId">
            <el-select v-model="data.form.parentVal" placeholder="选择上级部门（可选）" style="width: 420px;" clearable>
              <el-option v-for="d in data.parentOptions" :key="d._val" :label="d.name" :value="d._val" />
            </el-select>
          </el-form-item>

          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="data.form.description" placeholder="部门描述" rows="3" style="width: 420px;" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="data.updateFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import {hasPermission} from "@/utils/perm.js";

const data = reactive({
  name: null,
  pageNum: 1,
  pageSize: 10,
  total: 0,
  dept_list: [],
  deleteId: null,
  deleteName: '',
  updateFormVisible: false,
  formVisible3: false,
  form: {},
  ids: [],
  parentOptions: [], // for parent department select in form
});

const formRef = ref(null);

// membersMap: Map<departmentId, user[]>
const membersMap = new Map();

// panel: floating members panel state
const panel = reactive({
  visible: false,
  deptId: null,
  style: { top: '0px', left: '0px' },
});

// small hide timer to avoid flicker
let hideTimer = null;

// helper: format date
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

// get member names joined for inline display
const joinMemberNames = (dept) => {
  const list = membersMap.get(dept.id) || [];
  if (!list || list.length === 0) return '-';
  return list.map(u => u.username || u.userName || u.name || ('ID:' + u.id)).join('，');
};

// load departments (server-side paging)
const loadDepartments = async () => {
  try {
    const resp = await request.get('/department', {
      params: { name: data.name, pageNum: data.pageNum, pageSize: data.pageSize }
    });
    const body = resp.data || {};
    // support common PageInfo patterns
    let list = [];
    if (body.list && typeof body.total !== 'undefined') {
      list = body.list;
      data.total = body.total;
    } else if (body.data && body.data.list) {
      list = body.data.list;
      data.total = body.data.total || list.length;
    } else if (Array.isArray(body)) {
      list = body;
      data.total = list.length;
    } else {
      list = body.list || (body.data && body.data.list) || [];
      data.total = body.total || (body.data && body.data.total) || list.length;
    }
    data.dept_list = list.map(d => ({ ...d }));
    // build parentOptions (for add/edit)
    buildParentOptions(list);
  } catch (err) {
    console.error(err);
    ElMessage.error('加载部门失败');
    data.dept_list = [];
    data.total = 0;
  }
};

// load all users (for members mapping). We fetch once per page load; if dept filters change we still reuse
const loadAllUsers = async () => {
  try {
    // Ask a large pageSize to get all users; adjust if your users are huge
    const resp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 100000 } });
    const body = resp.data || {};
    const list = body.list || (body.data && body.data.list) || [];
    // group by department id
    membersMap.clear();
    for (const u of list) {
      const deptId = u.departmentId ?? u.department_id ?? (u.department && u.department.id);
      // skip if no dept
      if (deptId == null) continue;
      const id = Number(deptId);
      if (!membersMap.has(id)) membersMap.set(id, []);
      membersMap.get(id).push(u);
    }
  } catch (err) {
    console.warn('加载成员失败', err);
    membersMap.clear();
  }
};

// build parent options for form select
const buildParentOptions = (list) => {
  const map = new Map();
  list.forEach(d => {
    const _val = d.id != null ? String(d.id) : `name:${d.name}`;
    if (!map.has(_val)) map.set(_val, { _val, name: d.name });
  });
  data.parentOptions = Array.from(map.values());
};

// combined load
// 改为先加载 users（membersMap），再加载部门，这样列内容在首次渲染时即可显示成员名
const load = async () => {
  await loadAllUsers();
  await loadDepartments();
};

// search / reset
const onSearch = () => {
  data.pageNum = 1;
  load();
};
const restart = () => {
  data.name = null;
  data.pageNum = 1;
  data.pageSize = 10;
  load();
};

// add dialog
const handleAdd = () => {
  data.form = { name: '', parentVal: null, description: '' };
  data.updateFormVisible = true;
};

// handleUpdate: 打开编辑弹窗并填充表单（这是你请求的 update 操作入口）
const handleUpdate = (row) => {
  data.form = { ...row }; // shallow copy
  // 如果后端使用 parentId 而不是 parentVal，尝试填充 parentVal 以匹配表单 select 值
  if (data.form.parentId != null) {
    data.form.parentVal = String(data.form.parentId);
  } else {
    data.form.parentVal = null;
  }
  data.updateFormVisible = true;
};

// 独立的 update 请求函数（PUT），你说要加的“update 操作请求函数” — 可单独调用
const updateDepartment = async (id, payload) => {
  try {
    await request.put(`/department/${id}`, payload);
    ElMessage.success('更新成功');
    await load();
  } catch (err) {
    console.error('更新部门失败', err);
    ElMessage.error('更新失败');
    throw err;
  }
};

// prepare payload mapping parentVal -> parentId
const preparePayload = (form) => {
  const payload = { ...form };
  if (payload.parentVal == null) {
    payload.parentId = null;
  } else if (String(payload.parentVal).startsWith('name:')) {
    payload.parentId = null;
  } else {
    payload.parentId = Number(payload.parentVal);
  }
  delete payload.parentVal;
  return payload;
};

// save add/update
const save = async () => {
  if (!data.form.name || !data.form.name.trim()) {
    ElMessage.warning('请填写部门名称');
    return;
  }
  try {
    const payload = preparePayload(data.form);
    if (data.form.id) {
      // 使用 updateDepartment 专门函数或直接调用 PUT（两者效果一致）
      await updateDepartment(data.form.id, payload);
    } else {
      await request.post('/department', payload);
      ElMessage.success('添加成功');
      data.pageNum = 1;
      await load();
    }
    data.updateFormVisible = false;
  } catch (err) {
    console.error(err);
    // updateDepartment 已经会提示错误信息
    if (!err) ElMessage.error('保存失败');
  }
};

// delete single
const handleDel = (id) => {
  data.formVisible3 = true;
  data.deleteId = id;
  const found = data.dept_list.find(d => d.id === id);
  data.deleteName = found ? found.name : String(id);
};

const deleteData = async () => {
  try {
    await request.delete(`/department/${data.deleteId}`);
    ElMessage.success('删除成功');
    data.formVisible3 = false;
    await load();
  } catch (err) {
    console.error(err);
    ElMessage.error('删除失败');
  } finally {
    data.deleteId = null;
    data.deleteName = '';
  }
};

// selection & batch delete
const handleSelectionChange = (rows) => {
  data.ids = rows.map(r => r.id);
};

const delBatch = async () => {
  if (!data.ids || data.ids.length === 0) {
    ElMessage.warning('请选择数据');
    return;
  }
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除吗？', '确认', { type: 'warning' });
    await request.delete('/department/deleteBatch', { data: data.ids });
    ElMessage.success('删除成功');
    await load();
  } catch (err) {
    if (err !== 'cancel') console.error(err);
  }
};

// pagination
const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1;
  load();
};
const handleCurrentChange = (page) => {
  data.pageNum = page;
  load();
};

// ---------- members panel interaction logic ----------
const onMembersEnter = (event, dept) => {
  clearTimeout(hideTimer);
  const rect = event.currentTarget.getBoundingClientRect();
  const panelWidth = 300;
  const margin = 8;
  let left = rect.right + margin;
  if (left + panelWidth > window.innerWidth - 10) {
    left = rect.left - panelWidth - margin;
    if (left < 10) left = window.innerWidth - panelWidth - 10;
  }
  let top = rect.top;
  if (top + 400 > window.innerHeight) {
    top = Math.max(10, window.innerHeight - 420);
  }
  panel.style = { top: `${top}px`, left: `${left}px` };
  panel.deptId = dept.id;
  panel.visible = true;
};

const onMembersLeave = () => {
  clearTimeout(hideTimer);
  hideTimer = setTimeout(() => {
    panel.visible = false;
    panel.deptId = null;
  }, 180);
};

const onPanelEnter = () => {
  clearTimeout(hideTimer);
};
const onPanelLeave = () => {
  clearTimeout(hideTimer);
  hideTimer = setTimeout(() => {
    panel.visible = false;
    panel.deptId = null;
  }, 180);
};

// cleanup timer
onBeforeUnmount(() => {
  clearTimeout(hideTimer);
});

// initial load
load();
</script>

<style scoped>
.top-links-bar {
  margin: 0;
  background: #ffffff;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
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
  cursor: pointer;
}
.top-link.active {
  color: #1f6feb;
  font-weight: 500;
  border-bottom-color: #1f6feb;
}

/* 卡片 / 工具条 */
.merged-card {
  margin: 5px;
  background-color: white;
  padding: 10px;
  box-shadow: 0 0 12px rgba(0,0,0,.12);
}
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
.form-wrapper {
  padding: 10px 20px;
}

/* members inline: single line, ellipsis */
.members-cell {
  position: relative;
  max-width: 100%;
  outline: none;
}
.members-inline {
  display: inline-block;
  max-width: 420px; /* 控制一行最多显示宽度（可根据表格宽度调整） */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  cursor: default;
  color: #111827;
}

/* description ellipsis */
.desc-ellipsis {
  display: inline-block;
  max-width: 420px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #4b5563;
}

/* panel header and list styles (这些类会被 teleport 的元素使用) */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
}
.panel-title {
  font-weight: 600;
  color: #111827;
}
.panel-count {
  font-size: 12px;
  color: #6b7280;
}
.panel-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.panel-item {
  display: flex;
  flex-direction: column;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.12s;
}
.panel-item:hover {
  background: #f8fafc;
}
.panel-item-name {
  font-weight: 500;
  color: #0f172a;
}
.panel-item-email {
  font-size: 12px;
  color: #6b7280;
}
.panel-empty {
  color: #6b7280;
  font-size: 13px;
  padding: 8px;
}

/* 操作文字样式（替换原来的图标按钮） */
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
  color: #ef4444; /* 红色用于删除 */
}
.action-link.danger:hover {
  background-color: rgba(239,68,68,0.08);
}
</style>

<!-- 非 scoped 样式：确保 teleport 到 body 的 .members-panel 能正确应用，并实现动画 -->
<style>
.members-panel {
  position: fixed;
  z-index: 99999;
  width: 300px;
  max-height: 420px;
  background-color: #ffffff;     /* 确保不透明背景 */
  border-radius: 8px;
  box-shadow: 0 8px 28px rgba(17,24,39,0.12);
  padding: 12px;
  overflow: auto;
  border: 1px solid rgba(16,24,40,0.06);
  pointer-events: auto;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  will-change: top, left, transform;
  transition: opacity 0.6s ease, transform 0.6s ease;
}


.panel-fade-leave-active {
  transition: opacity 180ms cubic-bezier(.2,.8,.2,1), transform 180ms cubic-bezier(.2,.8,.2,1);
}
.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
}
.panel-fade-enter-to,
.panel-fade-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}
</style>
