import { reactive, ref, toRefs } from 'vue';

export default function () {
  // 1. 修正账号密码初始值：设为空，符合用户输入逻辑
  let account = ref(""); // 初始空字符串，用户输入时覆盖
  let password = ref(""); // 改为字符串类型，与后端接收类型一致

  // 2. 移除无用的 persion、dog 相关代码（未在登录中使用）
  // 3. 保留登录所需的响应式状态和方法
  let show = ref(true);
  let show1 = ref(false); // 控制“更多选项”下拉框
  let show2 = ref(true);

  // 关闭弹窗/隐藏元素的方法
  function submit() {
    show.value = false;
  }

  // 切换“更多选项”显示/隐藏
  function submit1() {
    show1.value = !show1.value; // 简化判断逻辑
  }

  function submit2() {
    show2.value = false;
  }

  // 返回登录组件需要的响应式数据和方法
  return {
    account,
    password,
    show,
    show1,
    submit,
    submit1,
    submit2,
    show2
  };
}