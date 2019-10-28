import Vue from 'vue';
import Vuex from 'vuex';
import apiStore from '@/services/api-store';
import workspacesStore from './services/workspaces-store';
import appStore from './services/app-store';
import i18nStore from './services/i18n-store';

Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    api: apiStore,
    workspaces: workspacesStore,
    app: appStore,
    i18n: i18nStore,
  },
});
