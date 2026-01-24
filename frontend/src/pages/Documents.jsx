import React from 'react'
import { useQuery } from 'react-query'
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  CircularProgress,
  Button,
} from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import DeleteIcon from '@mui/icons-material/Delete'
import VisibilityIcon from '@mui/icons-material/Visibility'
import RefreshIcon from '@mui/icons-material/Refresh'
import { dmsApi } from '../services/api'
import { format } from 'date-fns'
import { toast } from 'react-toastify'

function Documents() {
  const { data, isLoading, error, refetch } = useQuery('documents', () =>
    dmsApi.getDocuments()
  )

  const handleDownload = async (contentId, filename) => {
    try {
      const response = await dmsApi.downloadContent(contentId)
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename || 'document')
      document.body.appendChild(link)
      link.click()
      link.remove()
      toast.success('Download started')
    } catch (error) {
      toast.error('Failed to download document')
      console.error('Download error:', error)
    }
  }

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this document?')) {
      try {
        await dmsApi.deleteDocument(id)
        toast.success('Document deleted successfully')
        refetch()
      } catch (error) {
        toast.error('Failed to delete document')
        console.error('Delete error:', error)
      }
    }
  }

  if (isLoading) {
    return (
      <Box className="loading-spinner">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box>
        <Typography color="error">
          Error loading documents: {error.message}
        </Typography>
      </Box>
    )
  }

  const documents = data?.data || []

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Documents</Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => refetch()}
        >
          Refresh
        </Button>
      </Box>

      {documents.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="textSecondary">
            No documents found
          </Typography>
          <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
            Upload your first document to get started
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Size</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {documents.map((doc) => (
                <TableRow key={doc.id}>
                  <TableCell>{doc.name || 'Untitled'}</TableCell>
                  <TableCell>{doc.docType || 'N/A'}</TableCell>
                  <TableCell>
                    {doc.size ? `${(doc.size / 1024).toFixed(2)} KB` : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={doc.status || 'Active'}
                      color={doc.status === 'Active' ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {doc.created
                      ? format(new Date(doc.created), 'MMM dd, yyyy')
                      : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => console.log('View:', doc.id)}
                      title="View"
                    >
                      <VisibilityIcon />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleDownload(doc.contentId, doc.name)}
                      title="Download"
                    >
                      <DownloadIcon />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleDelete(doc.id)}
                      title="Delete"
                      color="error"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}

export default Documents
