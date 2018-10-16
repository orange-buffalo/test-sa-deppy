import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import {initApi, LOGIN_REQUIRED_EVENT, SUCCESSFUL_LOGIN_EVENT} from '@/services/api'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import EventBus from 'eventbusjs'

Vue.config.productionTip = false

Vue.use(ElementUI);

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')

initApi(store)

EventBus.addEventListener(LOGIN_REQUIRED_EVENT, () => {
  router.push('/login')
})

EventBus.addEventListener(SUCCESSFUL_LOGIN_EVENT, () => {
  store.dispatch('workspaces/loadWorkspaces').then(() => {
    if (!store.state.workspaces.currentWorkspace) {
      router.push('/workspace-setup')
    }
    else {
      router.push('/')
    }
  })
})