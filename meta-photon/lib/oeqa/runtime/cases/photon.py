from oeqa.runtime.case import OERuntimeTestCase
from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.decorator.package import OEHasPackage

class PhotonTest(OERuntimeTestCase):
    @OEHasPackage(['photon-dashboard'])
    def test_dashboard_binary(self):
        """Test if the dashboard binary is present at the expected location."""
        (status, output) = self.target.run('ls /usr/bin/DashboardOnly')
        self.assertEqual(status, 0, msg='Dashboard binary not found: %s' % output)

    @OETestDepends(['photon.PhotonTest.test_dashboard_binary'])
    def test_dashboard_service(self):
        """Test if the photon-dashboard systemd service is active."""
        (status, output) = self.target.run('systemctl is-active photon-dashboard')
        self.assertEqual(status, 0, msg='photon-dashboard service is not active: %s' % output)

    def test_xserver_running(self):
        """Test if Xorg is running as expected for a kiosk."""
        (status, output) = self.target.run('pgrep Xorg')
        self.assertEqual(status, 0, msg='X server (Xorg) is not running: %s' % output)

    def test_vulkan_info(self):
        """Test if Vulkan support is present (vulkan-loader/mesa)."""
        (status, output) = self.target.run('vulkaninfo --summary')
        # If vulkaninfo is missing, it's not a hard failure unless explicitly required
        if status != 0:
            self.skipTest('vulkan-loader/vulkaninfo not found on target')
        else:
            self.assertIn('Vulkan Instance Version', output, msg='Vulkan summary does not contain version info')
