import unittest
import tempfile
import os
import App
from flask import json


class APITestClass(unittest.TestCase):
    def setUp(self):
        self.db_fd, App.app.config['DATABASE'] = tempfile.mkstemp()
        App.app.config['TESTING'] = True
        self.app = App.app.test_client()
        App.init_db()

    def tearDown(self):
        os.close(self.db_fd)
        os.unlink(App.app.config['DATABASE'])
    
    def test_report(self):
        # Test empty query
        respone = self.app.post('/api/reports', content_type='application/json', data=json.dumps({}))
        self.assertEqual(respone.status_code,400)
       
        # Test valid query
        type = "Special"
        description = "Test report"
        address = "address"
        lat = "23728378273"
        lng = "37872387232"
        imagedata = "image data"
        respone = self.create_report(type, description, address, lat, lng, imagedata)
        self.assertNotEqual(respone.status_code,400)
        id = json.loads(respone.data)['reports']['id']

        # Check if report is in the database
        respone = self.app.get('/api/reports?page=1&perpage=200')
        self.assertEqual(respone.status_code,200)
        self.assertIn(id , [ r['id'] for r in json.loads(respone.data)['reports']])
        self.assertIn(description , [ r['description'] for r in json.loads(respone.data)['reports']])
        self.assertIn(int(lat) , [ r['lat'] for r in json.loads(respone.data)['reports']])

        # Test getting single report 
        respone = self.app.get('/api/reports/'+str(id))
        self.assertEqual(id , json.loads(respone.data)['reports']['id'])

        # Remove report from database
        respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':id}))
        self.assertEqual(respone.status_code,200)
        print(respone)
        
        # Check if report still exists
        respone = self.app.get('/api/reports')
        self.assertNotIn(id , [ r['id'] for r in json.loads(respone.data)['reports']])

        # Test deleting non existant report
        respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':id}))
        self.assertEqual(respone.status_code,404)
        
        # Test getting non existant single report
        respone = self.app.get('/api/reports/'+str(id))
        self.assertEqual(respone.status_code,404)

        # Test valid query with invalid report type
        type = "None"
        respone = self.create_report(type, description, address, lat, lng, imagedata)
        self.assertEqual(respone.status_code,400)
   
    def test_reports_limit(self):
        ids = []
        limit = 1000
        # create 1000 report
        for i in range(0,limit):
            type = "Special"
            description = "Test report"
            address = "address"
            lat = "-25.363"
            lng = "131.044"
            imagedata = "image data"
            respone = self.create_report(type, description, address, lat, lng, imagedata)
            self.assertNotEqual(respone.status_code,400)
            ids.append(json.loads(respone.data)['reports']['id'])

        # retreive 1000 reports one per page
        for i in range(0,limit):
            respone = self.app.get('/api/reports?page='+ str(i+1) +'&perpage=1')
            self.assertEqual(respone.status_code,200)
            self.assertIn(ids[i] , [ r['id'] for r in json.loads(respone.data)['reports']])
        
        # retreive 1000 reports 20 per page
        for i in range(0,limit//20):
            respone = self.app.get('/api/reports?page='+ str(i+1) +'&perpage=20')
            self.assertEqual(respone.status_code,200)
            for j in range(0,20):
                print("i:"+str(i))
                print("j:"+str(j))
                print(ids)
                print(json.loads(respone.data)['reports'])
                self.assertIn(ids[j+(i*20)] , [ r['id'] for r in json.loads(respone.data)['reports']])
        
        # delete 1000 reports
        for i in range(0,limit):
            respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':ids[i]}))
            self.assertEqual(respone.status_code,200)
            respone = self.app.get('/api/reports/'+str(ids[i]))
            self.assertEqual(respone.status_code,404)

    def create_report(self, type, description, address, lat, lng, imagedata):
        return self.app.post('/api/reports', data=json.dumps({'type':type, 'description':description,
                                                             'address':address, 'lat':lat, 'lng':lng, 'imagedata':imagedata}),
                            content_type='application/json')

if __name__ == '__main__':
    unittest.main()