<template>
  <div class="page-root">
    <!-- 左侧窄卡（类似二级菜单） -->
    <div class="left-card">
      <div class="left-top">
        <!-- 项目选择气泡（el-select 但样式像气泡） -->
        <el-select
            v-model="selectedProjectId"
            placeholder="选择项目"
            filterable
            clearable
            @change="onProjectChange"
            class="project-bubble"
        >
          <el-option
              v-for="p in projects"
              :key="p.id"
              :label="p.name"
              :value="p.id"
          />
        </el-select>
      </div>

      <div class="left-section">
        <div class="section-label">项目</div>
        <div
            class="section-item"
            :class="{ active: activeTab === 'basic' }"
            @click="activeTab = 'basic'"
        >
          <div class="item-title">基本信息</div>
          <div class="item-sub">查看/编辑项目基础信息</div>
        </div>
      </div>

      <div class="left-section">
        <div class="section-label">成员</div>
        <div
            class="section-item"
            :class="{ active: activeTab === 'members' }"
            @click="activeTab = 'members'"
        >
          <div class="item-title">成员管理</div>
          <div class="item-sub">管理项目成员</div>
        </div>
      </div>

      <div class="left-footer">
        <div class="small-muted">当前项目</div>
        <div class="current-project-name">{{ currentProject?.name || '-' }}</div>
      </div>
    </div>

    <!-- 右侧宽卡（内容区，切换 basic / members） -->
    <div class="right-card">
      <!-- 基本信息 卡片 -->
      <el-card v-if="activeTab === 'basic'" class="content-card">
        <div class="card-header">
          <div class="card-title">项目基本信息</div>
          <div class="card-actions">
            <el-button
                v-if="!editMode"
                type="primary"
                size="small"
                @click="enterEdit"
            >编辑</el-button>

            <template v-else>
              <el-button type="info" size="small" @click="cancelEdit">取消</el-button>
              <el-button type="primary" size="small" @click="saveProject">完成</el-button>
            </template>
          </div>
        </div>

        <div class="card-body basic-body" v-if="!editMode">
          <div class="info-row">
            <div class="label">名称</div>
            <div class="value">{{ currentProject?.name || '-' }}</div>
          </div>
          <div class="info-row">
            <div class="label">描述</div>
            <div class="value desc">{{ currentProject?.description || '-' }}</div>
          </div>
          <div class="info-row">
            <div class="label">模块</div>
            <div class="value">
              <div class="module-bubbles">
                <template v-if="modules && modules.length">
                  <span
                      class="module-bubble"
                      v-for="m in modules"
                      :key="m.id || m.name"
                      :title="m.name"
                  >
                    {{ m.name }}
                  </span>
                </template>
                <span v-else>-</span>
              </div>
            </div>
          </div>
          <div class="info-row">
            <div class="label">成员数</div>
            <div class="value">{{ membersTotal }}</div>
          </div>
          <div class="info-row">
            <div class="label">创建时间</div>
            <div class="value">{{ formatDate(currentProject?.createdAt) }}</div>
          </div>
        </div>

        <div class="card-body edit-body" v-else>
          <el-form :model="editForm" ref="editFormRef" label-width="90px" class="edit-form">
            <el-form-item label="名称" prop="name" :rules="[{ required: true, message: '请填写项目名称', trigger: 'blur' }]">
              <el-input v-model="editForm.name" placeholder="请输入项目名称"></el-input>
            </el-form-item>

            <el-form-item label="描述" prop="description">
              <el-input type="textarea" v-model="editForm.description" rows="4" placeholder="项目描述"></el-input>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 成员管理 卡片 -->
      <el-card v-if="activeTab === 'members'" class="content-card">
        <div class="card-header">
          <div class="card-title">成员管理</div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="openAddMember">添加成员</el-button>
          </div>
        </div>

        <div class="card-body members-body">
          <el-table :data="members" stripe style="width: 100%" @selection-change="onMemberSelectionChange">
            <el-table-column type="selection" width="55"></el-table-column>

            <el-table-column prop="username" label="用户名" min-width="100">
              <template #default="{ row }">
                <div class="member-name-cell">
                  <el-avatar v-if="row.avatarUrl" :src="row.avatarUrl" size="28" style="margin-right:8px"></el-avatar>
                  <span>{{ row.username || ('ID:' + row.id) }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="email" label="邮箱" min-width="150">
              <template #default="{ row }">
                <span>{{ row.email || '-' }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="roleName" label="角色" width="150">
              <template #default="{ row }">{{ row.roleName || '-' }}</template>
            </el-table-column>

            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <span class="action-link" @click="handleRemoveMember(row)">移除</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="table-footer">
            <el-pagination
                @size-change="memberSizeChange"
                @current-change="memberCurrentChange"
                :current-page="memberPageNum"
                :page-size="memberPageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="membersTotal"
            />
            <el-button type="danger" size="small" @click="batchRemoveMembers" style="margin-left: 12px">批量移除</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加成员对话框 -->
    <el-dialog title="添加成员到项目" v-model="addMemberVisible" width="600" destroy-on-close>
      <el-form :model="addMemberForm" label-width="100px">
        <el-form-item label="选择用户" prop="userId">
          <el-select v-model="addMemberForm.userId" filterable placeholder="选择用户">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.username || ('ID:' + u.id)" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addMemberForm.roleId" placeholder="选择角色（可选）">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display:flex; justify-content:flex-end; gap:10px">
          <el-button @click="addMemberVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddMember">添加</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import request from '@/utils/request.js';
import { ElMessage, ElMessageBox } from 'element-plus';

const projects = ref([]);
const selectedProjectId = ref(null);
const currentProject = ref(null);
const activeTab = ref('basic');

const editMode = ref(false);
const editFormRef = ref(null);
const editForm = reactive({ id: null, name: '', description: '' });

const modules = ref([]);

const members = ref([]);
const membersTotal = ref(0);
const memberPageNum = ref(1);
const memberPageSize = ref(10);
const memberSelected = ref([]);

const addMemberVisible = ref(false);
const addMemberForm = reactive({ userId: null, roleId: null });
const allUsers = ref([]);
const roles = ref([]);

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

const loadProjects = async () => {
  try {
    const resp = await request.get('/projects', { params: { pageNum: 1, pageSize: 1000 } });
    const body = resp.data || {};
    const data = body.data || body;
    const list = data.list || (Array.isArray(data) ? data : []);
    projects.value = list;
    if (!selectedProjectId.value && projects.value.length > 0) {
      selectedProjectId.value = projects.value[0].id;
      await onProjectChange(selectedProjectId.value);
    }
  } catch (err) {
    console.error('加载项目失败', err);
    ElMessage.error('加载项目失败');
  }
};

const loadProjectDetail = async (projectId) => {
  if (!projectId) {
    currentProject.value = null;
    modules.value = [];
    return;
  }
  try {
    const resp = await request.get(`/projects/${projectId}`);
    const body = resp.data || {};
    const p = body.data || body;
    currentProject.value = p || null;
    editForm.id = p?.id || null;
    editForm.name = p?.name || '';
    editForm.description = p?.description || '';
    await loadModules(projectId);
    memberPageNum.value = 1;
    await loadMembers(projectId);
  } catch (err) {
    console.error('加载项目详情失败', err);
    ElMessage.error('加载项目失败');
    currentProject.value = null;
  }
};

const loadModules = async (projectId) => {
  try {
    const resp = await request.get('/modules', { params: { projectId, pageNum: 1, pageSize: 1000 } });
    const body = resp.data || {};
    const data = body.data || body;
    const list = data.list || (Array.isArray(data) ? data : []);
    modules.value = list;
  } catch (err) {
    console.warn('加载模块失败', err);
    modules.value = [];
  }
};

const loadMembers = async (projectId) => {
  if (!projectId) {
    members.value = [];
    membersTotal.value = 0;
    return;
  }
  try {
    const resp = await request.get(`/projects/${projectId}/users`, {
      params: { pageNum: memberPageNum.value, pageSize: memberPageSize.value }
    });
    const raw = resp.data || {};
    const data = raw.data || raw;
    const list = data.list || (Array.isArray(data) ? data : []);
    const total = typeof data.total !== 'undefined' ? data.total : list.length;
    members.value = list;
    membersTotal.value = total;
  } catch (err) {
    console.error('加载成员失败', err);
    ElMessage.error('加载成员失败');
    members.value = [];
    membersTotal.value = 0;
  }
};

const loadAllUsers = async () => {
  try {
    const resp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 100000 } });
    const body = resp.data || {};
    const data = body.data || body;
    const list = data.list || (Array.isArray(data) ? data : []);
    allUsers.value = list;
  } catch (err) {
    console.warn('加载用户失败', err);
    allUsers.value = [];
  }
};

const loadRoles = async () => {
  try {
    const resp = await request.get('/role', { params: { pageNum: 1, pageSize: 1000 } });
    const body = resp.data || {};
    const data = body.data || body;
    roles.value = data.list || (Array.isArray(data) ? data : []);
  } catch (err) {
    roles.value = [];
  }
};

const onProjectChange = async (id) => {
  selectedProjectId.value = id;
  await loadProjectDetail(id);
  if (activeTab.value === 'members') {
    await loadMembers(id);
  }
};

const enterEdit = () => {
  if (!currentProject.value) return;
  editMode.value = true;
};

const cancelEdit = () => {
  editMode.value = false;
  editForm.name = currentProject.value?.name || '';
  editForm.description = currentProject.value?.description || '';
};

const saveProject = async () => {
  if (!editForm.name || !editForm.name.trim()) {
    ElMessage.warning('项目名称不能为空');
    return;
  }
  try {
    const payload = { name: editForm.name, description: editForm.description };
    await request.put(`/projects/${editForm.id}`, payload);
    ElMessage.success('更新成功');
    editMode.value = false;
    await loadProjects();
    await loadProjectDetail(editForm.id);
  } catch (err) {
    console.error('更新项目失败', err);
    ElMessage.error('更新失败');
  }
};

const onMemberSelectionChange = (rows) => { memberSelected.value = rows; };
const memberSizeChange = (size) => { memberPageSize.value = size; memberPageNum.value = 1; loadMembers(selectedProjectId.value); };
const memberCurrentChange = (page) => { memberPageNum.value = page; loadMembers(selectedProjectId.value); };

const openAddMember = async () => {
  await loadAllUsers();
  await loadRoles();
  addMemberForm.userId = null;
  addMemberForm.roleId = null;
  addMemberVisible.value = true;
};

const confirmAddMember = async () => {
  if (!addMemberForm.userId) {
    ElMessage.warning('请选择用户');
    return;
  }
  try {
    // 使用后端实际接口：POST /projects/{id}/users  body: { id: userId }
    await request.post(`/projects/${selectedProjectId.value}/users`, { id: addMemberForm.userId });
    ElMessage.success('添加成功');
    addMemberVisible.value = false;
    await loadMembers(selectedProjectId.value);
  } catch (err) {
    console.error('添加成员失败', err);
    ElMessage.error('添加失败');
  }
};

const batchRemoveMembers = async () => {
  if (!memberSelected.value || memberSelected.value.length === 0) {
    ElMessage.warning('请选择成员');
    return;
  }
  try {
    await ElMessageBox.confirm('确定要移除选中的成员吗？', '确认', { type: 'warning' });
    const ids = memberSelected.value.map(u => u.id);
    // 使用后端接口：DELETE /projects/{id}/users  body: [userId,...]
    await request.delete(`/projects/${selectedProjectId.value}/users`, { data: ids });
    ElMessage.success('移除成功');
    await loadMembers(selectedProjectId.value);
  } catch (err) {
    if (err !== 'cancel') {
      console.error('批量移除失败', err);
      ElMessage.error('操作失败');
    }
  }
};

const handleRemoveMember = async (row) => {
  try {
    await ElMessageBox.confirm(`确认将用户 ${row.username || row.id} 从项目中移除？`, '确认', { type: 'warning' });
    // 使用后端接口：DELETE /projects/{id}/users/{userId}
    await request.delete(`/projects/${selectedProjectId.value}/users/${row.id}`);
    ElMessage.success('移除成功');
    await loadMembers(selectedProjectId.value);
  } catch (err) {
    if (err !== 'cancel') {
      console.error('移除失败', err);
      ElMessage.error('移除失败');
    }
  }
};

onMounted(async () => {
  await loadProjects();
  loadRoles();
});
</script>

<style scoped>
/* 原样式保持不变（略） */
.page-root { display: flex; gap: 16px; padding: 12px; background: #f5f7fa; min-height: calc(100vh - 20px); }
.left-card { width: 15%; min-width: 260px; background: #ffffff; border-radius: 8px; padding: 14px; box-shadow: 0 6px 20px rgba(2,6,23,0.06); display: flex; flex-direction: column; gap: 16px; margin-bottom: 60px; }
.left-top { display:flex; align-items:center; justify-content:center; }
.project-bubble .el-select { width: 100%; }
.project-bubble .el-select .el-input__inner { border-radius: 999px; padding-left: 16px; padding-right: 12px; height: 40px; background: linear-gradient(180deg,#ffffff,#fbfdff); box-shadow: inset 0 1px 0 rgba(16,24,40,0.03); }
.section-label { color: #6b7280; font-size: 12px; margin-bottom: 8px; }
.section-item { padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: background .12s, transform .12s; background: transparent; }
.section-item:hover { background: rgba(31,111,235,0.04); transform: translateX(3px); }
.section-item.active { background: linear-gradient(90deg, rgba(31,111,235,0.08), rgba(31,111,235,0.04)); border-left: 3px solid #1f6feb; }
.item-title { font-weight: 600; color: #0f172a; }
.item-sub { color: #6b7280; font-size: 12px; margin-top: 4px; }
.left-footer { margin-top: auto; padding-top: 6px; border-top: 1px dashed #eef2ff; }
.small-muted { font-size: 12px; color: #94a3b8; }
.current-project-name { margin-top: 6px; font-weight: 600; color: #0f172a; }
.right-card { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 12px; margin-right: 30px; }
.content-card { width: 100%; padding: 12px 16px; border-radius: 10px; box-shadow: 0 6px 26px rgba(2,6,23,0.04); }
.card-header { display:flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-title { font-size: 18px; font-weight: 700; color: #0f172a; }
.card-actions { display:flex; gap:8px; }
.basic-body .info-row { display:flex; gap: 12px; padding: 12px 0; align-items: flex-start; border-bottom: 1px dashed rgba(15,23,42,0.04); }
.basic-body .info-row:last-child { border-bottom: none; }
.basic-body .label { width: 140px; color: #64748b; font-size: 14px; }
.basic-body .value { flex: 1; color: #0f172a; font-size: 15px; }
.basic-body .value.desc { white-space: pre-line; color: #374151; line-height: 1.6; }
.edit-form { padding-top: 6px; }
.members-body { display:flex; flex-direction: column; gap: 8px; }
.table-footer { display:flex; align-items:center; justify-content:flex-end; margin-top: 12px; }
.action-link { cursor: pointer; color: #1f6feb; margin-right: 10px; font-weight: 500; }
.member-name-cell { display:flex; align-items:center; }
.module-bubbles { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.module-bubble { display: inline-block; padding: 4px 12px; border-radius: 6px; background: linear-gradient(180deg, #fdfdfd 0%, #d9d9d9 100%); border: 1px solid rgba(180, 180, 180, 0.5); box-shadow: 0 1px 0 rgba(255,255,255,0.8) inset, 0 2px 6px rgba(0,0,0,0.08), 0 1px 3px rgba(255,255,255,0.4) inset; font-size: 13px; color: #1e293b; cursor: default; white-space: nowrap; max-width: 220px; text-overflow: ellipsis; overflow: hidden; transition: all 0.15s ease; }
.module-bubble:hover { background: linear-gradient(180deg, #ffffff 0%, #cfd2d4 100%); box-shadow: 0 1px 0 rgba(255,255,255,0.9) inset, 0 4px 12px rgba(0,0,0,0.12), 0 1px 4px rgba(255,255,255,0.6) inset; transform: translateY(-2px); }
@media (max-width: 1000px) { .page-root { flex-direction: column; } .left-card { width: 100%; min-width: auto; order: 1; } .right-card { order: 2; } }
</style>
