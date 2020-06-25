import useNotifications from '@/components/useNotifications';

export default {
  title: 'Components/Notifications',
};

export const Error = () => ({
  data() {
    return {
      showNotification() {
        const { showErrorNotification } = useNotifications();
        showErrorNotification({
          message: 'Some error happened, please try again later',
        });
      },
    };
  },
  template: '<ElButton @click="showNotification">Show</ElButton>',
  mounted() {
    this.showNotification();
  },
});
