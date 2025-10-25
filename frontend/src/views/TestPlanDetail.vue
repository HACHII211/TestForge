<template>
  <div class="page-root">
    <div class="top-bar small">
      <div class="crumbs">
        <a @click.prevent="goBack" class="link">测试计划</a> / <span class="current">{{ plan.name || '计划详情' }}</span>
      </div>
      <div class="top-actions">
        <el-button @click="refreshAll" plain>刷新</el-button>
      </div>
    </div>

    <div class="container">
      <!-- 左侧：计划基本信息 & 用例列表（固定高度） -->
      <div class="left-col card">
        <div class="plan-header">
          <div class="plan-title">{{ plan.name }}</div>
          <div class="plan-meta">
            <span>项目：{{ projectName || '-' }}</span>
            <span>负责人：{{ ownerName || '-' }}</span>
            <span>状态：<el-tag>{{ plan.status || '-' }}</el-tag></span>
          </div>
        </div>

        <div class="case-list" ref="caseList">
          <div v-if="cases.length === 0" class="empty">该计划还没有用例</div>

          <div
              v-for="c in cases"
              :key="c.id"
              :class="['case-item', { active: selectedCase && selectedCase.id === c.id }]"
              @click="selectCase(c)"
          >
            <div class="case-left">
              <div class="case-title">{{ c.title }}</div>
              <div class="case-meta small">{{ c.moduleName || c.module_name || '-' }}</div>
            </div>
            <div class="case-right">
              <el-tag :type="priorityTag(c.priority || c.priorityName)">{{ c.priority || c.priorityName || '-' }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：用例详情（固定高度，与左侧一致） -->
      <div class="right-col card">
        <!-- 右上：编辑 / 取消+保存 按钮（空心样式 plain） -->
        <div class="detail-actions-top">
          <template v-if="!editingCase">
            <el-button type="primary" plain @click="enterEdit">编辑</el-button>
          </template>
          <template v-else>
            <el-button type="primary" plain @click="cancelEdit">取消</el-button>
            <el-button type="primary" plain @click="saveEdit" style="margin-left:8px">保存</el-button>
          </template>
        </div>

        <div class="case-detail-scroll">
          <div v-if="selectedCase" class="case-detail">
            <div class="detail-header">
              <div class="detail-title">{{ selectedCase.title }}</div>
            </div>

            <div class="detail-body">
              <!-- 前置条件 -->
              <div class="field">
                <div class="field-label">前置条件</div>
                <div class="field-value" v-if="!editingCase">{{ selectedCase.preCondition || selectedCase.pre_condition || '-' }}</div>
                <div v-else>
                  <el-input type="textarea" v-model="editableCase.preCondition" rows="2" placeholder="前置条件" />
                </div>
              </div>

              <!-- 执行步骤 (非编辑显示原文; 编辑时显示可增删的步骤列表) -->
              <div class="field">
                <div class="field-label">执行步骤</div>
                <div class="field-value mono" v-if="!editingCase">{{ selectedCase.steps || selectedCase.step || '-' }}</div>
                <div v-else>
                  <div class="steps-list">
                    <div
                        v-for="(s, idx) in editableCase.stepsList"
                        :key="idx"
                        class="step-item"
                        style="display:flex; gap:8px; align-items:flex-start; margin-bottom:8px;"
                    >
                      <el-input
                          v-model="editableCase.stepsList[idx]"
                          placeholder="步骤描述"
                          style="flex:1"
                      />
                      <el-button
                          type="text"
                          title="删除此步骤"
                          @click="removeStep(idx)"
                          style="color:#e55353"
                      >✕</el-button>
                    </div>

                    <div style="margin-top:8px;">
                      <el-button type="primary" size="small" @click="addStep">添加步骤</el-button>
                      <span class="small" style="margin-left:10px;color:#6b7280">每行代表一条步骤，保存时会合并为换行分隔的文本</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 预期结果 -->
              <div class="field">
                <div class="field-label">预期结果</div>
                <div class="field-value" v-if="!editingCase">{{ selectedCase.expectedResult || selectedCase.expected_result || '-' }}</div>
                <div v-else>
                  <el-input type="textarea" v-model="editableCase.expectedResult" rows="3" placeholder="预期结果" />
                </div>
              </div>

              <!-- 其他信息（可编辑的元字段） -->
              <div class="field small">
                <div class="field-label">优先级</div>
                <div class="field-value" v-if="!editingCase">
                  <el-tag :type="priorityTag(selectedCase.priority || selectedCase.priorityName)">
                    {{ selectedCase.priority || selectedCase.priorityName || '-' }}
                  </el-tag>
                </div>
                <div v-else>
                  <el-select v-model="editableCase.priority" placeholder="选择优先级" style="width:200px">
                    <el-option label="High" value="High" />
                    <el-option label="Medium" value="Medium" />
                    <el-option label="Low" value="Low" />
                  </el-select>
                </div>
              </div>

              <div class="field small">
                <div class="field-label">模块</div>
                <div class="field-value" v-if="!editingCase">{{ selectedCase.moduleName || selectedCase.module_name || '-' }}</div>
                <div v-else>
                  <el-input v-model="editableCase.moduleName" placeholder="模块名字（自由文本）" style="width:240px" />
                </div>
              </div>

              <div class="field small">
                <div class="field-label">创建者</div>
                <div class="field-value">{{ selectedCase.createdByName || selectedCase.created_by_name || '-' }}</div>
              </div>

              <!-- 最近执行 -->
              <div class="field small">
                <div class="field-label">最近执行</div>
                <div class="field-value">
                  <div v-if="lastExecution">
                    <div>状态：<el-tag>{{ lastExecution.status }}</el-tag></div>
                    <div>执行人：{{ lastExecution.executedByName || lastExecution.executed_by_name || '-' }}</div>
                    <div>时间：{{ formatDate(lastExecution.executedAt || lastExecution.executed_at) }}</div>
                    <div>实际结果：<pre class="mono small">{{ lastExecution.actualResult || lastExecution.actual_result || '-' }}</pre></div>
                  </div>
                  <div v-else>-</div>
                </div>
              </div>
            </div>

          </div>

          <div v-else class="empty-state">请选择左侧用例查看详情</div>
        </div>

        <!-- 右下按钮（固定保留） -->
        <div class="detail-footer">
          <div class="left-note small">用例 ID: {{ selectedCase?.id || '-' }}</div>
          <div class="right-controls">
            <el-button type="primary" @click="openSubmitResult">提交结果</el-button>
            <el-button type="warning" @click="openAddDefect" style="margin-left:8px">添加缺陷</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 提交结果弹窗 -->
    <el-dialog title="提交执行结果" v-model="submitDialog.visible" width="640px">
      <el-form :model="submitDialog.form" label-width="110px">
        <el-form-item label="执行状态">
          <el-select v-model="submitDialog.form.status" placeholder="选择状态">
            <el-option label="Passed" value="Passed" />
            <el-option label="Failed" value="Failed" />
            <el-option label="Blocked" value="Blocked" />
          </el-select>
        </el-form-item>

        <el-form-item label="执行环境">
          <el-input v-model="submitDialog.form.environment" placeholder="例：chrome-118 / staging" />
        </el-form-item>

        <el-form-item label="实际结果">
          <el-input type="textarea" v-model="submitDialog.form.actualResult" rows="6" />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="submitDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="doSubmitResult">提交</el-button>
      </span>
    </el-dialog>

    <!-- 添加缺陷抽屉 -->
    <el-drawer v-model="defectDrawer.visible" :with-header="false" direction="rtl" :size="'46vw'" append-to-body :destroy-on-close="true">
      <div class="drawer-header">
        <div class="drawer-title">{{ defectDrawer.isEdit ? '编辑缺陷' : '新增缺陷' }}</div>
        <div class="drawer-actions"><el-button type="text" @click="defectDrawer.visible = false">关闭</el-button></div>
      </div>

      <div class="drawer-body">
        <el-form :model="defectDrawer.form" label-width="110px">
          <el-form-item label="标题">
            <el-input v-model="defectDrawer.form.title" />
          </el-form-item>

          <el-form-item label="优先级">
            <el-select v-model="defectDrawer.form.priority">
              <el-option label="High" value="High" />
              <el-option label="Medium" value="Medium" />
              <el-option label="Low" value="Low" />
            </el-select>
          </el-form-item>

          <el-form-item label="处理人">
            <el-select v-model="defectDrawer.form.assignedTo" clearable>
              <el-option v-for="u in users" :key="u.id" :label="u.username || u.name" :value="u.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="描述">
            <el-input type="textarea" v-model="defectDrawer.form.description" rows="8" />
          </el-form-item>
        </el-form>
      </div>

      <div class="drawer-footer">
        <el-button @click="defectDrawer.visible = false">取消</el-button>
        <el-button type="primary" @click="submitDefect">保存</el-button>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request.js';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();

const planId = Number(route.query.id || route.params.id);

// state
const plan = reactive({});
const cases = ref([]);
const selectedCase = ref(null);
const editableCase = reactive({
  // stepsList: will be created on enterEdit
});
const editingCase = ref(false);
const lastExecution = ref(null);

const projectName = ref('');
const ownerName = ref('');
const users = ref([]);

// load plan
const loadPlan = async () => {
  try {
    const resp = await request.get(`/test-plans/${planId}`);
    const dataObj = resp?.data?.data ?? resp?.data ?? {};
    Object.assign(plan, dataObj);

    // after we have plan, try load project name if projectId exists
    const pid = plan.projectId ?? plan.project_id ?? null;
    if (pid) {
      await loadProject(pid);
    }
  } catch (err) {
    console.error(err);
  }
};

// ----- 1) 新增： loadProject(projectId) -----
const loadProject = async (projectId) => {
  if (!projectId) {
    projectName.value = '';
    return;
  }
  try {
    const resp = await request.get(`/projects/${projectId}`);
    const data = resp?.data?.data ?? resp?.data ?? {};
    // try common keys
    projectName.value = data.name ?? data.projectName ?? data.title ?? '';
  } catch (err) {
    console.warn('loadProject error', err);
    projectName.value = '';
  }
};

// load plan cases (ids -> fetch details)
const loadPlanCases = async () => {
  try {
    const resp = await request.get(`/test-plans/${planId}/testcases`);
    const body = resp.data || {};
    const idArr = body.data ?? body;
    const ids = Array.isArray(idArr) ? idArr : (Array.isArray(body) ? body : []);
    const promises = ids.map(id =>
        request.get(`/testcases/${id}`).then(r => {
          const d = r.data?.data ?? r.data ?? {};
          return d;
        }).catch(() => null)
    );
    const results = await Promise.all(promises);
    cases.value = results.filter(Boolean);
    if (cases.value.length) {
      selectCase(cases.value[0]);
    } else {
      selectedCase.value = null;
      lastExecution.value = null;
    }
  } catch (err) {
    console.error(err);
    cases.value = [];
  }
};

// load users from corrected route
const loadUsers = async () => {
  try {
    const resp = await request.get('/organization/users', { params: { pageNum: 1, pageSize: 500 } });
    const list = resp.data?.data?.list ?? resp.data?.list ?? resp.data ?? [];
    users.value = list.map(u => ({ id: u.id, username: u.username || u.name }));
    // map ownerName if plan has ownerId
    if (plan.ownerId) {
      const owner = users.value.find(u => Number(u.id) === Number(plan.ownerId));
      ownerName.value = owner ? owner.username : '';
    }
  } catch (err) {
    console.warn('loadUsers error', err);
    users.value = [];
  }
};

// When plan.ownerId changes, try to update ownerName
watch(() => plan.ownerId, () => {
  const u = users.value.find(x => Number(x.id) === Number(plan.ownerId));
  ownerName.value = u ? u.username : '';
});

// select case and load last execution
const selectCase = async (c) => {
  selectedCase.value = c;
  // reset edit state
  editingCase.value = false;
  Object.keys(editableCase).forEach(k => delete editableCase[k]);
  await loadLastExecution(c.id);
};

const loadLastExecution = async (caseId) => {
  try {
    const resp = await request.get('/executions', { params: { testCaseId: caseId, pageNum: 1, pageSize: 1 } });
    const page = resp.data?.data ?? resp.data ?? {};
    const arr = page.list ?? page;
    lastExecution.value = Array.isArray(arr) && arr.length ? arr[0] : null;
  } catch (err) {
    console.error(err);
    lastExecution.value = null;
  }
};

// navigation & refresh
const goBack = () => router.back();
const refreshAll = async () => {
  await loadPlan();
  await loadUsers();      // corrected route
  await loadPlanCases();
  if (selectedCase.value) await loadLastExecution(selectedCase.value.id);
};

// ----- 编辑逻辑 -----
const enterEdit = () => {
  if (!selectedCase.value) return;
  // copy selectedCase -> editableCase (shallow copy)
  Object.assign(editableCase, JSON.parse(JSON.stringify(selectedCase.value)));
  // prepare stepsList from existing steps text (split by newline)
  const raw = editableCase.steps ?? editableCase.step ?? '';
  let arr = [];
  if (raw && typeof raw === 'string') {
    arr = raw.split(/\r?\n/).map(s => s.trim()).filter(s => s.length > 0);
  }
  if (arr.length === 0) arr = ['']; // at least one empty input for usability
  // set stepsList (reactive)
  editableCase.stepsList = arr;
  editingCase.value = true;
};

const cancelEdit = () => {
  // revert changes
  Object.keys(editableCase).forEach(k => delete editableCase[k]);
  editingCase.value = false;
};

// step list helpers
const addStep = () => {
  if (!Array.isArray(editableCase.stepsList)) editableCase.stepsList = [];
  editableCase.stepsList.push('');
  // scroll or focus could be added
};

const removeStep = (index) => {
  if (!Array.isArray(editableCase.stepsList)) return;
  editableCase.stepsList.splice(index, 1);
  if (editableCase.stepsList.length === 0) editableCase.stepsList.push('');
};

const saveEdit = async () => {
  if (!editableCase.id) return;
  try {
    // prepare steps: join stepsList by newline, trimming and keeping non-empty lines
    const stepsList = Array.isArray(editableCase.stepsList)
        ? editableCase.stepsList.map(s => (s || '').toString().trim()).filter(s => s.length > 0)
        : [];
    const stepsText = stepsList.join('\n');

    // prepare payload - map common fields
    const payload = {
      title: editableCase.title,
      preCondition: editableCase.preCondition ?? editableCase.pre_condition,
      steps: stepsText,
      expectedResult: editableCase.expectedResult ?? editableCase.expected_result,
      priority: editableCase.priority,
      moduleId: editableCase.moduleId ?? editableCase.module_id
    };
    // call backend update (PUT /testcases/{id})
    await request.put(`/testcases/${editableCase.id}`, payload);
    ElMessage.success('保存成功');
    // merge back to selectedCase
    Object.assign(selectedCase.value, JSON.parse(JSON.stringify(editableCase)));
    // ensure displayed steps reflect joined text
    selectedCase.value.steps = stepsText;
    editingCase.value = false;
  } catch (err) {
    console.error(err);
    ElMessage.error('保存失败');
  }
};

// ----- 提交结果 -----
const submitDialog = reactive({
  visible: false,
  form: {
    status: 'Passed',
    environment: '',
    actualResult: ''
  }
});
const openSubmitResult = () => {
  if (!selectedCase.value) {
    ElMessage.warning('请选择用例');
    return;
  }
  submitDialog.visible = true;
};

const doSubmitResult = async () => {
  try {
    // get current user id from localStorage 'auth.user'
    let executedBy = null;
    try {
      const rawUser = localStorage.getItem('auth.user') || sessionStorage.getItem('auth.user');
      const parsed = rawUser ? JSON.parse(rawUser) : null;
      if (parsed && (parsed.id || parsed.userId || parsed.user_id)) {
        executedBy = parsed.id ?? parsed.userId ?? parsed.user_id;
      }
    } catch (e) {
      console.warn('read auth.user failed', e);
    }

    const payload = {
      testCaseId: selectedCase.value.id,
      executedBy: executedBy,
      executedAt: null,
      status: submitDialog.form.status,
      remarks: null,
      environment: submitDialog.form.environment,
      actualResult: submitDialog.form.actualResult
    };
    await request.post('/executions', payload);
    ElMessage.success('提交成功');
    submitDialog.visible = false;
    await loadLastExecution(selectedCase.value.id);
  } catch (err) {
    console.error(err);
    ElMessage.error('提交失败');
  }
};

// ----- 添加缺陷 -----
const defectDrawer = reactive({
  visible: false,
  isEdit: false,
  form: {
    title: '',
    description: '',
    priority: 'Medium',
    assignedTo: null,
    status: 'New'
  }
});

const openAddDefect = () => {
  if (!selectedCase.value) {
    ElMessage.warning('请选择用例');
    return;
  }
  defectDrawer.isEdit = false;
  defectDrawer.form = {
    title: `来自计划 ${plan.name || ''} 的缺陷 - ${selectedCase.value.title}`,
    description: '',
    priority: 'Medium',
    assignedTo: null,
    status: 'New'
  };
  defectDrawer.visible = true;
};

const submitDefect = async () => {
  try {
    if (!defectDrawer.form.title || !defectDrawer.form.title.trim()) {
      ElMessage.warning('请填写缺陷标题');
      return;
    }
    const payload = {
      title: defectDrawer.form.title,
      description: defectDrawer.form.description,
      priority: defectDrawer.form.priority,
      assignedTo: defectDrawer.form.assignedTo
    };
    const resp = await request.post('/defects', payload);
    const created = resp.data?.data ?? resp.data ?? {};
    ElMessage.success('缺陷已创建');

    // 尝试关联缺陷到用例（若后端提供该接口）
    try {
      const defectId = created.id ?? created.data?.id;
      if (defectId) {
        await request.post('/testcase-defects', { testCaseId: selectedCase.value.id, defectId });
      }
    } catch (err) {
      // 忽略关联错误
    }

    defectDrawer.visible = false;
  } catch (err) {
    console.error(err);
    ElMessage.error('创建缺陷失败');
  }
};

// utils
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

const priorityTag = (p) => {
  if (!p) return '';
  if (String(p).toLowerCase().startsWith('h')) return 'danger';
  if (String(p).toLowerCase().startsWith('m')) return 'warning';
  return 'success';
};

onMounted(async () => {
  if (!planId) {
    ElMessage.error('缺少计划 ID');
    return;
  }
  await loadPlan();
  await loadUsers();      // corrected route
  await loadPlanCases();
  if (selectedCase.value) await loadLastExecution(selectedCase.value.id);
});
</script>

<style scoped>
.page-root { padding: 16px; background: #f7fafc; min-height: 100vh; box-sizing: border-box; }
.top-bar.small { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; color:#374151; }
.crumbs .link { color:#1f6feb; cursor:pointer; margin-right:6px; }
.container { display:flex; gap:12px; align-items:flex-start; /* 固定高度：保证左右同高 */ min-height: calc(100vh - 160px); margin-bottom: 12px; }

/* 左侧固定宽度卡片 */
.left-col { width:320px; flex-shrink:0; display:flex; flex-direction:column; gap:12px; padding:14px; box-sizing:border-box; height: calc(100vh - 220px); overflow:hidden; }
.right-col { flex:1; min-width:0; padding:14px; box-sizing:border-box; height: calc(100vh - 220px); display:flex; flex-direction:column; }

/* 卡片基础 */
.card { background:#fff; border-radius:12px; box-shadow:0 8px 22px rgba(15,23,42,0.06); }

/* 左侧内容 */
.plan-header { margin-bottom:8px; }
.plan-title { font-size:18px; font-weight:700; color:#111827; }
.plan-meta { color:#6b7280; display:flex; gap:10px; margin-top:6px; font-size:13px; }

/* 用例列表：内部滚动 */
.case-list { display:flex; flex-direction:column; gap:8px; margin-top:8px; overflow:auto; padding-right:6px; }

/* case item */
.case-item { display:flex; justify-content:space-between; align-items:center; padding:10px; border-radius:10px; cursor:pointer; background:rgba(15,23,42,0.02); transition:all .12s ease; border:1px solid rgba(15,23,42,0.03); }
.case-item:hover { transform: translateY(-2px); box-shadow:0 8px 20px rgba(15,23,42,0.04); }
.case-item.active { background: linear-gradient(90deg, rgba(31,111,235,0.06), rgba(255,255,255,0)); border-color: rgba(31,111,235,0.12); }
.case-left { display:flex; flex-direction:column; gap:4px; }
.case-title { font-weight:600; color:#0f172a; }
.case-meta.small { color:#6b7280; font-size:12px; }

/* 右侧顶部按钮区域（右对齐） */
.detail-actions-top { display:flex; justify-content:flex-end; gap:8px; margin-bottom:8px; }

/* 右侧详情滚动区域 */
.case-detail-scroll { overflow:auto; padding-right:6px; flex:1; }

/* 详情主体 */
.case-detail { display:flex; flex-direction:column; gap:12px; }
.detail-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.detail-title { font-size:18px; font-weight:700; color:#111827; }
.detail-body { display:flex; flex-direction:column; gap:12px; }
.field { display:flex; gap:12px; align-items:flex-start; }
.field-label { width:140px; color:#6b7280; font-weight:600; }
.field-value { flex:1; color:#111827; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Roboto Mono", "Courier New", monospace; white-space:pre-wrap; background: #fafafa; padding:8px; border-radius:6px; border:1px solid rgba(15,23,42,0.03); }

/* step list item */
.step-item el-input { width:100%; }

/* 底部控制 */
.detail-footer { display:flex; justify-content:space-between; align-items:center; margin-top:12px; }
.detail-footer .left-note { color:#6b7280; font-size:13px; }
.right-controls { display:flex; gap:8px; }

/* 空状态 */
.empty-state { display:flex; align-items:center; justify-content:center; min-height:200px; color:#6b7280; font-size:14px; }

/* drawer & dialog 共用样式 */
.drawer-header { display:flex; align-items:center; justify-content:space-between; padding:14px 20px; border-bottom:1px solid rgba(0,0,0,0.04); }
.drawer-title { font-weight:600; font-size:16px; }
.drawer-body { padding:18px 28px 80px; max-height:calc(100% - 160px); overflow:auto; }
.drawer-footer { position:absolute; right:24px; bottom:18px; display:flex; gap:10px; z-index:30; }

/* responsive */
@media (max-width: 960px) {
  .container { flex-direction:column; min-height: auto; }
  .left-col, .right-col { width:100%; height: auto; max-height: none; }
}
</style>


<style scoped>
/* （原来的样式保持不变） */
.page-root { padding: 16px; background: #f7fafc; min-height: 100vh; box-sizing: border-box; }
.top-bar.small { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; color:#374151; }
.crumbs .link { color:#1f6feb; cursor:pointer; margin-right:6px; }
.container { display:flex; gap:12px; align-items:flex-start; min-height: calc(100vh - 160px); margin-bottom: 12px; }
.left-col { width:320px; flex-shrink:0; display:flex; flex-direction:column; gap:12px; padding:14px; box-sizing:border-box; height: calc(100vh - 220px); overflow:hidden; }
.right-col { flex:1; min-width:0; padding:14px; box-sizing:border-box; height: calc(100vh - 220px); display:flex; flex-direction:column; }
.card { background:#fff; border-radius:12px; box-shadow:0 8px 22px rgba(15,23,42,0.06); }
.plan-header { margin-bottom:8px; }
.plan-title { font-size:18px; font-weight:700; color:#111827; }
.plan-meta { color:#6b7280; display:flex; gap:10px; margin-top:6px; font-size:13px; }
.case-list { display:flex; flex-direction:column; gap:8px; margin-top:8px; overflow:auto; padding-right:6px; }
.case-item { display:flex; justify-content:space-between; align-items:center; padding:10px; border-radius:10px; cursor:pointer; background:rgba(15,23,42,0.02); transition:all .12s ease; border:1px solid rgba(15,23,42,0.03); }
.case-item:hover { transform: translateY(-2px); box-shadow:0 8px 20px rgba(15,23,42,0.04); }
.case-item.active { background: linear-gradient(90deg, rgba(31,111,235,0.06), rgba(255,255,255,0)); border-color: rgba(31,111,235,0.12); }
.case-left { display:flex; flex-direction:column; gap:4px; }
.case-title { font-weight:600; color:#0f172a; }
.case-meta.small { color:#6b7280; font-size:12px; }
.detail-actions-top { display:flex; justify-content:flex-end; gap:8px; margin-bottom:8px; }
.case-detail-scroll { overflow:auto; padding-right:6px; flex:1; }
.case-detail { display:flex; flex-direction:column; gap:12px; }
.detail-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.detail-title { font-size:18px; font-weight:700; color:#111827; }
.detail-body { display:flex; flex-direction:column; gap:12px; }
.field { display:flex; gap:12px; align-items:flex-start; }
.field-label { width:140px; color:#6b7280; font-weight:600; }
.field-value { flex:1; color:#111827; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Roboto Mono", "Courier New", monospace; white-space:pre-wrap; background: #fafafa; padding:8px; border-radius:6px; border:1px solid rgba(15,23,42,0.03); }
.detail-footer { display:flex; justify-content:space-between; align-items:center; margin-top:12px; }
.detail-footer .left-note { color:#6b7280; font-size:13px; }
.right-controls { display:flex; gap:8px; }
.empty-state { display:flex; align-items:center; justify-content:center; min-height:200px; color:#6b7280; font-size:14px; }
.drawer-header { display:flex; align-items:center; justify-content:space-between; padding:14px 20px; border-bottom:1px solid rgba(0,0,0,0.04); }
.drawer-title { font-weight:600; font-size:16px; }
.drawer-body { padding:18px 28px 80px; max-height:calc(100% - 160px); overflow:auto; }
.drawer-footer { position:absolute; right:24px; bottom:18px; display:flex; gap:10px; z-index:30; }

/* 新增的步骤行样式 */
.step-row { display:flex; gap:8px; align-items:center; margin-bottom:6px; }
.step-row .el-input { flex:1; }

/* responsive */
@media (max-width: 960px) {
  .container { flex-direction:column; min-height: auto; }
  .left-col, .right-col { width:100%; height: auto; max-height: none; }
}
</style>
