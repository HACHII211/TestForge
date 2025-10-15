<template>
  <div>
    <!-- 顶部导航 -->
    <div class="top-links-bar">
      <div class="top-links-inner">
        <a class="top-link" href="/testforge/organization">组织管理</a>
        <a class="top-link" href="/testforge/department">部门管理</a>
        <a class="top-link active" href="/testforge/permission">权限管理</a>
      </div>
    </div>

    <div class="card merged-card">
      <!-- 工具条：左侧只有 添加 按钮 -->
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">添加</el-button>
        </div>

        <div class="toolbar-right">
          <el-button type="warning" @click="load">刷新</el-button>
          <el-button type="danger" @click="delBatch">批量删除</el-button>
        </div>
      </div>

      <!-- 角色表格 -->
      <el-table :data="data.role_list" @selection-change="handleSelectionChange" style="margin-top: 12px">
        <el-table-column type="selection" width="55"></el-table-column>

        <el-table-column prop="id" label="ID" width="90" />

        <el-table-column prop="name" label="角色名称" width="190"/>

        <el-table-column prop="description" label="描述" />

        <el-table-column label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt || scope.row.created_at) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220">
          <template #default="scope">
            <!-- 改为纯文字操作，保持原行为与间距 -->
            <span class="action-link" @click="handleEdit(scope.row)">编辑</span>
            <span class="action-link" @click="openAssignPermissions(scope.row)">变更权限</span>
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog title="角色信息" v-model="data.editVisible" width="520" :destroy-on-close="true">
      <el-form :model="data.form" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="data.form.name" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="data.form.description" placeholder="请输入角色描述" type="textarea" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.editVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRole">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗：左侧用复选列表（从 /permission 获取），右侧显示当前角色信息 -->
    <el-dialog title="分配权限" v-model="data.permVisible" width="800" :destroy-on-close="true">
      <div style="display:flex; gap:16px;">
        <!-- 左侧：权限复选列表（带搜索 + 全选） -->
        <div style="flex:1; min-width:300px;">
          <div class="perm-header">
            <div style="font-weight:500;">权限列表（可多选）</div>
            <div class="perm-actions">
              <el-input
                  v-model="permSearch"
                  placeholder="搜索权限或描述"
                  size="small"
                  clearable
                  @clear="onPermSearch"
                  @input="onPermSearch"
                  style="width: 220px;"
              />
              <a class="perm-select-all" @click="toggleSelectAll">{{ allSelected ? '取消全选' : '全选' }}</a>
            </div>
          </div>

          <div class="perm-list-wrapper">
            <el-checkbox-group v-model="data.checkedPermIds">
              <div v-for="perm in filteredPermissions" :key="perm.id" class="perm-item">
                <el-checkbox :label="perm.id">
                  <span class="perm-name">{{ perm.name }}</span>
                  <div class="perm-desc">{{ perm.description }}</div>
                </el-checkbox>
              </div>

              <div v-if="filteredPermissions.length === 0" class="perm-empty">未找到权限</div>
            </el-checkbox-group>
          </div>
        </div>

        <!-- 右侧：当前角色信息 -->
        <div style="width:320px;">
          <div style="margin-bottom:8px; font-weight:500;">当前角色</div>
          <el-card>
            <div><strong>ID:</strong> {{ data.currentRole?.id || '-' }}</div>
            <div style="margin-top:6px;"><strong>名称:</strong> {{ data.currentRole?.name || '-' }}</div>
            <div style="margin-top:6px;"><strong>描述:</strong> {{ data.currentRole?.description || '-' }}</div>
          </el-card>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.permVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRolePermissions">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog title="警告" v-model="data.confirmDeleteVisible" width="420">
      <div style="text-align:center; font-size:16px; margin:20px 0;">
        确定删除角色 ID: <strong>{{ data.deleteId }}</strong> ?
      </div>
      <div style="display:flex; justify-content:flex-end; gap:10px;">
        <el-button @click="data.confirmDeleteVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确定删除</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';

const data = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
  role_list: [],
  ids: [],
  editVisible: false,
  permVisible: false,
  confirmDeleteVisible: false,
  deleteId: null,
  form: {},

  // permission dialog state
  permissionList: [],        // 平坦权限数组，从 /permission 获取
  checkedPermIds: [],        // 当前选中的权限 id 列表（数字）
  currentRole: null,
});

// 搜索关键字（权限列表）
const permSearch = ref('');

// 方便判断全选状态
const allSelected = computed(() => {
  return data.permissionList.length > 0 && data.checkedPermIds.length === data.permissionList.length;
});

// 过滤后的权限（支持名字/描述搜索）
const filteredPermissions = computed(() => {
  const kw = (permSearch.value || '').trim().toLowerCase();
  if (!kw) return data.permissionList;
  return data.permissionList.filter(p => {
    return String(p.name || '').toLowerCase().includes(kw) || String(p.description || '').toLowerCase().includes(kw);
  });
});

// 格式化时间
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

// 加载角色（分页）
const load = async () => {
  try {
    const resp = await request.get('/role', {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
      },
    });
    const body = resp.data || {};
    if (body.list && typeof body.total !== 'undefined') {
      data.role_list = body.list;
      data.total = body.total;
    } else if (body.data && body.data.list) {
      data.role_list = body.data.list;
      data.total = body.data.total || 0;
    } else if (Array.isArray(body)) {
      data.role_list = body;
      data.total = body.length;
    } else {
      data.role_list = body.list || [];
      data.total = body.total || (data.role_list.length);
    }
  } catch (err) {
    console.error(err);
    ElMessage.error('加载角色失败');
    data.role_list = [];
    data.total = 0;
  }
};

// 打开新增弹窗
const handleAdd = () => {
  data.form = { name: '', description: '' };
  data.editVisible = true;
};

// 编辑角色
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row || {}));
  data.editVisible = true;
};

// 保存角色（新增或更新）
const saveRole = async () => {
  if (!data.form.name) {
    ElMessage.warning('请填写角色名称');
    return;
  }
  try {
    if (data.form.id) {
      await request.put(`/role/${data.form.id}`, data.form);
      ElMessage.success('更新成功');
    } else {
      await request.post('/role', data.form);
      ElMessage.success('添加成功');
    }
    data.editVisible = false;
    data.pageNum = 1;
    await load();
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

// 删除：打开确认
const handleDel = (id) => {
  data.deleteId = id;
  data.confirmDeleteVisible = true;
};

// 确认删除
const confirmDelete = async () => {
  try {
    await request.delete(`/role/${data.deleteId}`);
    ElMessage.success('删除成功');
    data.confirmDeleteVisible = false;
    await load();
  } catch (err) {
    console.error(err);
    ElMessage.error('删除失败');
  } finally {
    data.deleteId = null;
  }
};

// 批量删除
const handleSelectionChange = (rows) => {
  data.ids = rows.map(r => r.id);
};

const delBatch = async () => {
  if (!data.ids || data.ids.length === 0) {
    ElMessage.warning('请选择要删除的角色');
    return;
  }
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除吗？', '确认', { type: 'warning' });
    await Promise.all(data.ids.map(id => request.delete(`/role/${id}`)));
    ElMessage.success('批量删除成功');
    data.ids = [];
    await load();
  } catch (err) {
    if (err !== 'cancel') console.error(err);
  }
};

// 分页回调
const handleSizeChange = (size) => {
  data.pageSize = size;
  data.pageNum = 1;
  load();
};

const handleCurrentChange = (page) => {
  data.pageNum = page;
  load();
};

// ========== 权限相关 ==========
/**
 * 从 /permission 接口加载完整权限列表（平坦数组）
 * 这里兼容多种后端包装：{ data: { list: [...] } } 或 { data: [...] } 或直接返回数组
 */
const loadPermissionList = async () => {
  try {
    const resp = await request.get('/permission');
    const body = resp.data || {};
    let list = [];
    if (body.data && Array.isArray(body.data.list)) {
      list = body.data.list;
    } else if (body.data && Array.isArray(body.data)) {
      list = body.data;
    } else if (Array.isArray(body)) {
      list = body;
    } else if (body.list && Array.isArray(body.list)) {
      list = body.list;
    } else {
      // fallback: try body.data.list or []
      list = (body.data && body.data.list) || (body.list) || [];
    }
    // ensure ids are numbers
    data.permissionList = list.map(p => ({
      id: Number(p.id),
      name: p.name,
      description: p.description || '',
    }));
  } catch (err) {
    console.warn('加载权限列表失败', err);
    data.permissionList = [];
  }
};

// 打开分配权限弹窗：加载权限列表 & 当前角色权限
const openAssignPermissions = async (role) => {
  data.currentRole = role;
  data.permVisible = true;

  // load permissions each time to keep latest (可根据需要缓存)
  await loadPermissionList();

  // load current role permissions (后端返回可能是 id 数组或者对象数组)
  try {
    const resp = await request.get(`/role/${role.id}/permissions`);
    const body = resp.data || {};
    let perms = [];
    if (Array.isArray(body)) perms = body;
    else if (body.data && Array.isArray(body.data)) perms = body.data;
    else if (Array.isArray(body.list)) perms = body.list;
    else perms = body.data || body.list || body || [];
    // normalize to number ids
    data.checkedPermIds = (perms || []).map(p => Number(p));
  } catch (err) {
    console.error('加载角色权限失败', err);
    data.checkedPermIds = [];
  }
};

// 保存角色权限
const saveRolePermissions = async () => {
  try {
    // data.checkedPermIds 已是数组（数字）
    await request.post(`/role/${data.currentRole.id}/permissions`, data.checkedPermIds);
    ElMessage.success('权限保存成功');
    data.permVisible = false;
  } catch (err) {
    console.error(err);
    ElMessage.error('保存权限失败');
  }
};

// 搜索回调
const onPermSearch = () => {
  // filteredPermissions 是 computed，会自动响应 permSearch 的变化
};

// 全选 / 取消全选
const toggleSelectAll = () => {
  if (allSelected.value) {
    data.checkedPermIds = [];
  } else {
    data.checkedPermIds = data.permissionList.map(p => p.id);
  }
};

// init
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
  border-bottom: 2px solid transparent;
  cursor: pointer;
}
.top-link.active {
  color: #1f6feb;
  font-weight: 500;
  border-bottom-color: #1f6feb;
}
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
}
.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0;
}

/* --- 操作文字样式（替换按钮） --- */
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

/* --- 权限列表样式 --- */
.perm-header {
  display:flex;
  justify-content:space-between;
  align-items:center;
  gap:8px;
  margin-bottom:8px;
}
.perm-actions {
  display:flex;
  align-items:center;
  gap:8px;
}
.perm-select-all {
  cursor:pointer;
  color:#374151;
  font-size:13px;
  margin-left:8px;
  text-decoration:underline;
}
.perm-list-wrapper {
  border: 1px solid #eef2f7;
  border-radius: 6px;
  padding: 10px;
  max-height: 420px;
  overflow: auto;
  background: #ffffff;
}
.perm-item {
  padding: 6px 6px;
  border-radius: 6px;
  transition: background .12s ease;
}
.perm-item:hover {
  background: #f8fafc;
}
.perm-name {
  font-weight: 500;
  font-size: 13px;
  margin-right: 6px;
}
.perm-desc {
  display:block;
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}
.perm-empty {
  padding: 12px;
  color: #9ca3af;
  text-align: center;
}
</style>
