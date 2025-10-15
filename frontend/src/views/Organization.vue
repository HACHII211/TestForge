<template>
  <div>
    <!-- 顶部导航条（无外层 margin）-->
    <div class="top-links-bar">
      <div class="top-links-inner">
        <a class="top-link active" href="/testforge/organization">组织管理</a>
        <a class="top-link" href="/testforge/department">部门管理</a>
        <a class="top-link" href="/testforge/permission">权限管理</a>
      </div>
    </div>

    <!-- 合并后的单个卡片：包含搜索区 + 操作按钮 + 表格 + 分页 -->
    <div class="card merged-card">
      <!-- 查询与筛选行 -->
      <div class="toolbar">
        <el-input
            v-model="data.username"
            placeholder="请输入用户名搜索"
            style="width: 300px"
            @keydown.enter="onSearch"
        ></el-input>

        <el-select v-model="data.filterDepartmentVal" placeholder="按部门筛选" clearable style="width: 220px; margin-left: 10px">
          <el-option v-for="d in data.departments" :key="d._val" :label="d.name" :value="d._val" />
        </el-select>

        <el-select v-model="data.filterRoleVal" placeholder="按角色筛选" clearable style="width: 180px; margin-left: 10px">
          <el-option v-for="r in data.roles" :key="r._val" :label="r.name" :value="r._val" />
        </el-select>

        <el-button type="primary" style="margin-left: 10px" @click="onSearch">查 询</el-button>
        <el-button type="success" style="margin-left: 10px" @click="restart">重 置</el-button>

        <div class="toolbar-right">
          <el-button type="danger" @click="delBatch">批量删除</el-button>
          <el-button type="primary" @click="handleAdd" style="margin-left: 10px">添加</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="data.emp_list" @selection-change="handleSelectionChange" style="margin-top: 12px">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column label="ID" prop="id" width="80"></el-table-column>

        <el-table-column label="用户名">
          <template #default="scope">
            {{ scope.row.username || scope.row.userName || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="邮箱" >
          <template #default="scope">
            {{ scope.row.email || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="部门" width="120">
          <template #default="scope">
            {{ getDeptName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column label="角色" width="120">
          <template #default="scope">
            {{ getRoleName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">激活</el-tag>
            <el-tag v-else-if="scope.row.status === 0" type="warning">禁用</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.created_at || scope.row.createdAt) }}
          </template>
        </el-table-column>

<!--        <el-table-column label="更新时间" width="180">-->
<!--          <template #default="scope">-->
<!--            {{ formatDate(scope.row.updated_at || scope.row.updatedAt) }}-->
<!--          </template>-->
<!--        </el-table-column>-->

        <el-table-column label="操作" width="160">
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
        您确定要删除编号为 {{ data.deleteId }} 的用户吗？
      </div>
      <div style="margin-top: auto; display: flex; justify-content: flex-end; gap: 10px;">
        <el-button type="info" @click="data.formVisible3=false">取消</el-button>
        <el-button type="info" @click="deleteData">确认删除</el-button>
      </div>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog title="用户信息" v-model="data.updateFormVisible" width="600" destroy-on-close>
      <div class="form-wrapper">
        <el-form :model="data.form" ref="formRef" label-width="120px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="data.form.username" placeholder="请输入用户名" style="width: 360px;" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="data.form.password" placeholder="请输入密码" style="width: 360px;" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="data.form.email" placeholder="请输入邮箱" style="width: 360px;" />
          </el-form-item>

          <el-form-item label="部门" prop="departmentId">
            <el-select v-model="data.form.departmentVal" placeholder="选择部门" style="width: 360px;" clearable>
              <el-option v-for="d in data.departments" :key="d._val" :label="d.name" :value="d._val" />
            </el-select>
          </el-form-item>

          <el-form-item label="角色" prop="roleId">
            <el-select v-model="data.form.roleVal" placeholder="选择角色" style="width: 360px;" clearable>
              <el-option v-for="r in data.roles" :key="r._val" :label="r.name" :value="r._val" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态" prop="status">
            <el-select v-model="data.form.status" placeholder="请选择状态" style="width: 360px;">
              <el-option label="激活" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
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
import { reactive, ref } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit } from '@element-plus/icons-vue';

const data = reactive({
  username: null,
  filterDepartmentVal: null,
  filterRoleVal: null,
  pageNum: 1,
  pageSize: 10,
  total: 0,
  emp_list: [],
  all_users: [],
  deleteId: null,
  updateFormVisible: false,
  formVisible3: false,
  form: {},
  ids: [],
  departments: [],
  roles: [],
});

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

const getDeptName = (row) => {
  if (!row) return '-';
  return row.departmentName || (row.department && (row.department.name || row.department.departmentName)) || row.department_name || '-';
};

const getRoleName = (row) => {
  if (!row) return '-';
  return row.roleName || (row.role && row.role.name) || row.role_name || '-';
};

const buildDeptAndRoleLists = (list) => {
  const deptMap = new Map();
  const roleMap = new Map();
  list.forEach(u => {
    const deptId = u.departmentId ?? u.department_id ?? (u.department && u.department.id);
    const deptName = u.departmentName ?? (u.department && (u.department.name || u.department.departmentName)) ?? u.department_name;
    if (deptId != null || deptName) {
      const _val = deptId != null ? String(deptId) : `name:${deptName}`;
      if (!deptMap.has(_val)) deptMap.set(_val, { id: deptId ?? null, name: deptName ?? '未知', _val });
    }
    const roleId = u.roleId ?? u.role_id ?? (u.role && u.role.id);
    const roleName = u.roleName ?? (u.role && u.role.name) ?? u.role_name;
    if (roleId != null || roleName) {
      const _val = roleId != null ? String(roleId) : `name:${roleName}`;
      if (!roleMap.has(_val)) roleMap.set(_val, { id: roleId ?? null, name: roleName ?? '未知', _val });
    }
  });
  data.departments = Array.from(deptMap.values());
  data.roles = Array.from(roleMap.values());
};

const applyFiltersAndPaginate = (list, pageNum, pageSize) => {
  let filtered = list.slice();
  if (data.filterDepartmentVal) {
    filtered = filtered.filter(u => {
      const id = u.departmentId ?? u.department_id ?? (u.department && u.department.id);
      const candidate = id != null ? String(id) : `name:${u.departmentName ?? (u.department && (u.department.name || u.department.departmentName)) ?? u.department_name}`;
      return candidate === data.filterDepartmentVal;
    });
  }
  if (data.filterRoleVal) {
    filtered = filtered.filter(u => {
      const id = u.roleId ?? u.role_id ?? (u.role && u.role.id);
      const candidate = id != null ? String(id) : `name:${u.roleName ?? (u.role && u.role.name) ?? u.role_name}`;
      return candidate === data.filterRoleVal;
    });
  }
  data.total = filtered.length;
  const start = (pageNum - 1) * pageSize;
  return filtered.slice(start, start + pageSize);
};

const load = async () => {
  try {
    if (data.filterDepartmentVal || data.filterRoleVal) {
      const allResp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 100000 } });
      const body = allResp.data || {};
      const list = body.list || (body.data && body.data.list) || [];
      data.all_users = list;
      buildDeptAndRoleLists(list);
      data.emp_list = applyFiltersAndPaginate(list, data.pageNum, data.pageSize);
    } else {
      const resp = await request.get('/organization/users', { params: { username: data.username, pageNum: data.pageNum, pageSize: data.pageSize } });
      const body = resp.data || {};
      const list = body.list || (body.data && body.data.list) || [];
      data.emp_list = list;
      data.total = body.total || (body.data && body.data.total) || list.length;
      buildDeptAndRoleLists(body.list || (body.data && body.data.list) || list);
    }
  } catch (err) {
    console.error(err);
    ElMessage.error('加载用户列表失败');
    data.emp_list = [];
    data.total = 0;
  }
};

const onSearch = () => {
  data.pageNum = 1;
  load();
};

const restart = () => {
  data.username = null;
  data.filterDepartmentVal = null;
  data.filterRoleVal = null;
  data.pageNum = 1;
  data.pageSize = 10;
  load();
};

const handleAdd = () => {
  data.updateFormVisible = true;
  data.form = { username: '', password: '', email: '', status: 1, departmentVal: null, roleVal: null };
};

const preparePayload = (form) => {
  const payload = { ...form };
  if (payload.departmentVal == null) {
    payload.departmentId = null;
  } else if (String(payload.departmentVal).startsWith('name:')) {
    payload.departmentId = null;
  } else {
    payload.departmentId = Number(payload.departmentVal);
  }
  if (payload.roleVal == null) {
    payload.roleId = null;
  } else if (String(payload.roleVal).startsWith('name:')) {
    payload.roleId = null;
  } else {
    payload.roleId = Number(payload.roleVal);
  }
  delete payload.departmentVal;
  delete payload.roleVal;
  return payload;
};

const save = async () => {
  if (!data.form.username) {
    ElMessage.warning('请填写用户名');
    return;
  }
  try {
    const payload = preparePayload(data.form);
    if (data.form.id) {
      await request.put(`/organization/users/${data.form.id}`, payload);
      ElMessage.success('更新成功');
    } else {
      await request.post('/organization/users', payload);
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
    await request.delete(`/organization/users/${data.deleteId}`);
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
  const deptId = data.form.departmentId ?? data.form.department_id ?? (data.form.department && data.form.department.id);
  const deptName = data.form.departmentName ?? (data.form.department && (data.form.department.name || data.form.department.departmentName)) ?? data.form.department_name;
  data.form.departmentVal = deptId != null ? String(deptId) : (deptName ? `name:${deptName}` : null);
  const roleId = data.form.roleId ?? data.form.role_id ?? (data.form.role && data.form.role.id);
  const roleName = data.form.roleName ?? (data.form.role && data.form.role.name) ?? data.form.role_name;
  data.form.roleVal = roleId != null ? String(roleId) : (roleName ? `name:${roleName}` : null);
  if (data.form.password) delete data.form.password;
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
    await request.delete('/organization/deleteBatch', { data: data.ids });
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

// 初始加载
load();
</script>

<style scoped>
/* 顶部条：白色背景、无外边距 */
.top-links-bar {
  margin: 0;
  background: #ffffff;               /* 不透明白色 */
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;  /* 轻微分隔线 */
  -webkit-tap-highlight-color: transparent; /* 移动端去掉高亮 */
}

/* 内部容器：左对齐，平铺排列 */
.top-links-inner {
  display: flex;
  justify-content: flex-start;
  gap: 18px;
  align-items: center;
  padding-left: 12px;
}

/* 链接：灰色、细体、无下划线、无 outline */
.top-link {
  color: #6b7280;            /* 静态灰色文本 */
  text-decoration: none;     /* 去掉下划线 */
  font-weight: 400;          /* 细一点 */
  font-size: 14px;
  line-height: 20px;
  padding: 6px 0;            /* 让下边线定位更稳 */
  transition: color 0.15s ease, border-color 0.15s ease;
  border-bottom: 2px solid transparent; /* 预留下划线位置，避免布局跳动 */
  -webkit-tap-highlight-color: transparent;
  cursor: pointer;
}

/* 悬停/聚焦都不要下划线，只稍微变色 */
.top-link:hover,
.top-link:focus {
  color: #4b5563;            /* 略深的灰色 */
  text-decoration: none;
  outline: none;
  box-shadow: none;
}

/* 被选中样式：颜色加深 + 细实线（subtle） */
.top-link.selected,
.top-link.active {
  color: #1f6feb;            /* 可调整为你项目主色 */
  font-weight: 500;          /* 略粗以示区分 */
  border-bottom-color: #1f6feb;
  padding-bottom: 4px;       /* 给下划线留出空间 */
}

/* 防止 link 被点击时产生默认 focus 边框（无障碍仍保留）*/
.top-link:focus {
  outline: none;
}

/* 合并后的卡片样式 */
.merged-card {
  margin: 5px; /* 你原来卡片的 margin */
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

</style>
