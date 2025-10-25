<template>
  <div class="testcase-card">
    <div class="header">
      <span class="id">{{ testCase.case_id }}</span>
      <span class="title">{{ testCase.title }}</span>
    </div>
    <div class="section">
      <strong>前置条件：</strong>
      <ul>
        <li v-for="(pre, i) in testCase.preconditions" :key="i">{{ pre }}</li>
        <li v-if="!testCase.preconditions.length">（无）</li>
      </ul>
    </div>
    <div class="section">
      <strong>步骤：</strong>
      <ol>
        <li v-for="step in testCase.steps" :key="step.step_number">
          {{ step.action }}
          <span v-if="step.data"> — {{ step.data }}</span>
        </li>
      </ol>
    </div>
    <div class="section">
      <strong>预期结果：</strong>
      <p>{{ testCase.expected_result }}</p>
    </div>
    <div class="section" v-if="testCase.actual_result !== undefined">
      <strong>实际结果：</strong>
      <p>{{ testCase.actual_result || '—' }}</p>
    </div>
    <div class="footer">
      <span class="priority">优先级：{{ testCase.priority || '中' }}</span>
      <span class="tags">
        标签：
        <template v-if="testCase.tags && testCase.tags.length">
          <span class="tag" v-for="(t, i) in testCase.tags" :key="i">{{ t }}</span>
        </template>
        <span v-else>—</span>
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TestCaseCard',
  props: {
    testCase: { type: Object, required: true },
  },
}
</script>

<style scoped>
.testcase-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fafafa;
}
.header {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}
.id {
  background: #10a37f;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}
.title {
  font-weight: 600;
  font-size: 14px;
}
.section {
  margin-bottom: 8px;
}
.section strong {
  display: block;
  margin-bottom: 4px;
}
.section ul,
.section ol {
  margin: 0 0 0 16px;
  padding: 0;
}
.footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #555;
  border-top: 1px solid #e6e6e6;
  padding-top: 8px;
}
.tag {
  display: inline-block;
  background: #e1f5fe;
  color: #0277bd;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 4px;
  font-size: 12px;
}
</style>
